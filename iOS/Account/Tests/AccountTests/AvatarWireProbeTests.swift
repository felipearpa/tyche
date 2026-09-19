import Foundation
import Testing
import UIKit
@testable import Account

// Opt-in and read-only: GETs a public avatar with the production request shape through
// `URLSession.shared` and reports what crossed the wire. It uploads nothing, and only the test
// runner's own URLCache is written. Enable it with TEST_RUNNER_AVATAR_WIRE_PROBE_ACCOUNT_ID.
private let probeAccountId = ProcessInfo.processInfo.environment["AVATAR_WIRE_PROBE_ACCOUNT_ID"] ?? ""

@Suite("Avatar wire probe (live object, read-only)", .serialized)
struct AvatarWireProbeTests {

    @Test(
        "given URLCache holds the avatar then the production request revalidates without transferring the body",
        .enabled(if: !probeAccountId.isEmpty, "set TEST_RUNNER_AVATAR_WIRE_PROBE_ACCOUNT_ID")
    )
    func productionRequestRevalidatesConditionally() async throws {
        let request = try #require(avatarPhotoRequest(accountId: probeAccountId))
        let recorder = WireRecorder()

        var deliveredSizes: [Int] = []
        for attempt in 1...3 {
            let (data, response) = try await URLSession.shared.data(for: request, delegate: recorder)
            let http = try #require(response as? HTTPURLResponse)
            deliveredSizes.append(data.count)
            print("WIRE-PROBE raw attempt=\(attempt) delivered=\(http.statusCode) bytes=\(data.count)")
            // URLCache commits asynchronously.
            try await Task.sleep(nanoseconds: 2_000_000_000)
        }

        let exchanges = recorder.exchanges
        exchanges.enumerated().forEach { index, exchange in
            print("WIRE-PROBE raw exchange[\(index)] \(exchange)")
        }

        try #require(exchanges.count == 3, "the per-task delegate received no task metrics")
        // The caller always receives the whole JPEG, however little crossed the wire.
        #expect(Set(deliveredSizes).count == 1)
        for exchange in exchanges.dropFirst() {
            #expect(exchange.networkBodyBytes == 0, "re-downloaded: \(exchange)")
        }
    }

    @Test(
        "given the live object is unchanged then the store revalidates and reloads without a body or a replacement",
        .enabled(if: !probeAccountId.isEmpty, "set TEST_RUNNER_AVATAR_WIRE_PROBE_ACCOUNT_ID")
    )
    func storeAgainstLiveObject() async throws {
        let recorder = WireRecorder()
        let clock = ProbeClock()
        let replacements = ProbeReplacements()
        let store = AvatarImageStore(
            fetch: { request in try await URLSession.shared.data(for: request, delegate: recorder) },
            cachedResponse: { request in URLCache.shared.cachedResponse(for: request) },
            now: { clock.now },
            notifyReplacement: { accountId in replacements.append(accountId) }
        )

        // 1. Ordinary load at the toolbar size.
        let toolbar = await store.image(accountId: probeAccountId, targetPixelSize: 96)
        #expect(toolbar != nil)
        try await Task.sleep(nanoseconds: 2_000_000_000)
        let afterFirstLoad = recorder.exchanges.count

        // 2. Fresh validation, a size not decoded yet: the URLCache fast path, no network.
        let profile = await store.image(accountId: probeAccountId, targetPixelSize: 288)
        #expect(profile != nil)
        #expect(recorder.exchanges.count == afterFirstLoad, "a fresh new-size load reached the network")

        // 3. Stale memory hit: one background revalidation, no replacement.
        clock.advance(by: avatarFreshnessInterval + 60)
        let stale = await store.image(accountId: probeAccountId, targetPixelSize: 96)
        #expect(stale === toolbar)
        await store.awaitRevalidations()
        #expect(recorder.exchanges.count == afterFirstLoad + 1)
        #expect(replacements.values.isEmpty, "an unchanged object was treated as replaced")

        // 4. Stale memory miss: the reload must not transfer the body, and must still reach
        //    the store as a usable 200.
        clock.advance(by: avatarFreshnessInterval + 60)
        await store.evictAllDecodedImages()
        let reloaded = await store.image(accountId: probeAccountId, targetPixelSize: 96)
        #expect(reloaded != nil, "a revalidated reload reached the store as unusable")

        let exchanges = recorder.exchanges
        exchanges.enumerated().forEach { index, exchange in
            print("WIRE-PROBE store exchange[\(index)] \(exchange)")
        }
        for exchange in exchanges.dropFirst(afterFirstLoad) {
            #expect(exchange.networkBodyBytes == 0, "re-downloaded: \(exchange)")
        }
    }
}

private struct WireExchange: CustomStringConvertible {
    struct Transaction {
        let fetchType: String
        let wireStatus: Int?
        let sentIfNoneMatch: Bool
        let headerBytes: Int64
        let bodyBytes: Int64
    }

    let transactions: [Transaction]

    var networkBodyBytes: Int64 {
        transactions.filter { $0.fetchType == "network" }.map(\.bodyBytes).reduce(0, +)
    }

    var description: String {
        transactions.map { transaction in
            let status = transaction.wireStatus.map(String.init) ?? "-"
            return "\(transaction.fetchType) status=\(status) if-none-match=\(transaction.sentIfNoneMatch) " +
                "headers=\(transaction.headerBytes)B body=\(transaction.bodyBytes)B"
        }.joined(separator: " | ")
    }
}

private final class WireRecorder: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    private let lock = NSLock()
    private var recorded: [WireExchange] = []

    var exchanges: [WireExchange] {
        lock.withLock { recorded }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didFinishCollecting metrics: URLSessionTaskMetrics) {
        let transactions = metrics.transactionMetrics.map { transaction in
            WireExchange.Transaction(
                fetchType: Self.name(of: transaction.resourceFetchType),
                wireStatus: (transaction.response as? HTTPURLResponse)?.statusCode,
                sentIfNoneMatch: transaction.request.value(forHTTPHeaderField: "If-None-Match") != nil,
                headerBytes: transaction.countOfResponseHeaderBytesReceived,
                bodyBytes: transaction.countOfResponseBodyBytesReceived
            )
        }
        lock.withLock { recorded.append(WireExchange(transactions: transactions)) }
    }

    private static func name(of fetchType: URLSessionTaskMetrics.ResourceFetchType) -> String {
        switch fetchType {
        case .networkLoad: "network"
        case .localCache: "local-cache"
        case .serverPush: "server-push"
        case .unknown: "unknown"
        @unknown default: "unknown"
        }
    }
}

private final class ProbeClock: @unchecked Sendable {
    private let lock = NSLock()
    private var current = Date()

    var now: Date {
        lock.withLock { current }
    }

    func advance(by interval: TimeInterval) {
        lock.withLock { current = current.addingTimeInterval(interval) }
    }
}

private final class ProbeReplacements: @unchecked Sendable {
    private let lock = NSLock()
    private var accountIds: [String] = []

    var values: [String] {
        lock.withLock { accountIds }
    }

    func append(_ accountId: String) {
        lock.withLock { accountIds.append(accountId) }
    }
}
