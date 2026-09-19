import CryptoKit
import Foundation
import Testing
import UIKit
@testable import Account

@Suite("AvatarImageStore")
struct AvatarImageStoreTests {

    @Test("given a fresh decoded entry then another request is served from memory without URL access")
    func freshEntryAvoidsURLAccess() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"a\"")))
        let store = fixture.makeStore()

        let first = await store.image(accountId: "gambler", targetPixelSize: 128)
        let second = await store.image(accountId: "gambler", targetPixelSize: 128)

        #expect(first != nil)
        #expect(second === first)
        #expect(fixture.fetcher.requestCount == 1)
    }

    @Test("given concurrent consumers missing the cache then one load serves every consumer")
    func concurrentConsumersCoalesceIntoOneLoad() async {
        let fixture = Fixture()
        fixture.fetcher.gated = true
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"a\"")))
        let store = fixture.makeStore()

        let first = Task { await store.image(accountId: "gambler", targetPixelSize: 128) }
        let second = Task { await store.image(accountId: "gambler", targetPixelSize: 128) }
        await waitUntil { fixture.fetcher.requestCount == 1 }
        fixture.fetcher.releaseGate()

        let firstImage = await first.value
        let secondImage = await second.value

        #expect(firstImage != nil)
        #expect(secondImage === firstImage)
        #expect(fixture.fetcher.requestCount == 1)
    }

    @Test("given a small render target then the decode is bounded by its size bucket, not the source size")
    func decodesAtTheRequestedBucket() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"a\"")))
        let store = fixture.makeStore()

        let image = await store.image(accountId: "gambler", targetPixelSize: 120)

        let cgImage = try! #require(image?.cgImage)
        #expect(max(cgImage.width, cgImage.height) == 128)
    }

    @Test("given a cached larger variant then a smaller request reuses it without another load")
    func largerVariantSatisfiesSmallerRequest() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"a\"")))
        let store = fixture.makeStore()

        let large = await store.image(accountId: "gambler", targetPixelSize: 512)
        let small = await store.image(accountId: "gambler", targetPixelSize: 96)

        #expect(small === large)
        #expect(fixture.fetcher.requestCount == 1)
    }

    @Test("given decoded entries beyond the 16 MiB budget then least-recently-used entries are evicted")
    func decodedCostStaysWithinBudget() async {
        let fixture = Fixture()
        fixture.fetcher.fallback = { _ in .success(jpegResponse(etag: "\"any\"")) }
        let store = fixture.makeStore()

        for index in 0..<20 {
            _ = await store.image(accountId: "gambler-\(index)", targetPixelSize: 512)
        }

        let cost = await store.accountedCost
        #expect(cost <= 16 * 1024 * 1024)

        // The earliest account was evicted, so it loads again; the latest is still in memory.
        let countBefore = fixture.fetcher.requestCount
        _ = await store.image(accountId: "gambler-0", targetPixelSize: 512)
        #expect(fixture.fetcher.requestCount == countBefore + 1)
        _ = await store.image(accountId: "gambler-19", targetPixelSize: 512)
        #expect(fixture.fetcher.requestCount == countBefore + 1)
    }

    @Test("given memory pressure eviction then a later request recovers from the HTTP cache without a network request")
    func memoryPressureEvictionRecoversFromURLCache() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"a\"")))
        fixture.cachedResponses["gambler"] = jpegResponse(etag: "\"a\"")
        let store = fixture.makeStore()

        let first = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(first != nil)

        await store.evictAllDecodedImages()
        let cost = await store.accountedCost
        #expect(cost == 0)

        let recovered = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(recovered != nil)
        #expect(fixture.fetcher.requestCount == 1)
    }

    @Test("given a missing avatar then concurrent surfaces share one not-found result for the staleness interval")
    func cachedAbsenceSuppressesRepeatedNotFound() async {
        let fixture = Fixture()
        fixture.fetcher.fallback = { _ in .success(notFoundResponse()) }
        let store = fixture.makeStore()

        let first = await store.image(accountId: "gambler", targetPixelSize: 128)
        let second = await store.image(accountId: "gambler", targetPixelSize: 128)

        #expect(first == nil)
        #expect(second == nil)
        #expect(fixture.fetcher.requestCount == 1)

        fixture.clock.now = fixture.clock.now.addingTimeInterval(6 * 60)
        _ = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(fixture.fetcher.requestCount == 2)
    }

    @Test("given a stale entry whose remote object is unchanged then validation refreshes without replacing the image")
    func unchangedRevalidationKeepsDecodedImage() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"same\"")))
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"same\"")))
        let store = fixture.makeStore()

        let original = await store.image(accountId: "gambler", targetPixelSize: 128)
        fixture.clock.now = fixture.clock.now.addingTimeInterval(6 * 60)

        let stale = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(stale === original)

        await store.awaitRevalidations()
        #expect(fixture.fetcher.requestCount == 2)
        #expect(fixture.replacements.values.isEmpty)

        let afterRevalidation = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(afterRevalidation === original)
        #expect(fixture.fetcher.requestCount == 2)
    }

    @Test("given a stale entry whose remote object changed then the image is replaced and consumers are notified")
    func changedRevalidationReplacesImageAndNotifies() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"one\"", color: .systemRed)))
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"two\"", color: .systemBlue)))
        let store = fixture.makeStore()

        let original = await store.image(accountId: "gambler", targetPixelSize: 128)
        fixture.clock.now = fixture.clock.now.addingTimeInterval(6 * 60)

        let stale = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(stale === original)

        await store.awaitRevalidations()
        #expect(fixture.replacements.values == ["gambler"])

        let replaced = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(replaced != nil)
        #expect(replaced !== original)
        #expect(fixture.fetcher.requestCount == 2)
    }

    @Test("given a load at a size not decoded yet meets a replaced photo then every older variant is superseded and consumers are notified")
    func ordinaryLoadMeetingAReplacedPhotoSupersedesOlderVariants() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"one\"", color: .systemRed)))
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"two\"", color: .systemBlue)))
        let store = fixture.makeStore()

        let toolbar = await store.image(accountId: "gambler", targetPixelSize: 96)
        fixture.clock.now = fixture.clock.now.addingTimeInterval(6 * 60)

        // Profile's size was never decoded, so this is an ordinary load, not a revalidation.
        let profile = await store.image(accountId: "gambler", targetPixelSize: 288)
        #expect(profile != nil)
        #expect(fixture.replacements.values == ["gambler"])

        // The toolbar's variant came from the replaced photo: it must not be served again,
        // nor be certified later by a revalidation that only knows the new ETag.
        let toolbarAfterwards = await store.image(accountId: "gambler", targetPixelSize: 96)
        #expect(toolbarAfterwards != nil)
        #expect(toolbarAfterwards !== toolbar)
        await store.awaitRevalidations()
        #expect(fixture.fetcher.requestCount == 2)
    }

    @Test("given revalidation fails offline then the stale image stays available and a later access retries")
    func offlineRevalidationRetainsStaleImage() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"a\"")))
        fixture.fetcher.fallback = { _ in .failure(URLError(.notConnectedToInternet)) }
        let store = fixture.makeStore()

        let original = await store.image(accountId: "gambler", targetPixelSize: 128)
        fixture.clock.now = fixture.clock.now.addingTimeInterval(6 * 60)

        let stale = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(stale === original)
        await store.awaitRevalidations()

        let afterFailure = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(afterFailure === original)
        await store.awaitRevalidations()
        #expect(fixture.fetcher.requestCount == 3)
        #expect(fixture.replacements.values.isEmpty)
    }

    @Test("given a successful upload then the seeded local image serves every compatible surface without a GET")
    func uploadSeedServesAllSurfacesWithoutNetwork() async {
        let fixture = Fixture()
        let store = fixture.makeStore()
        let uploaded = uiImage(side: 512, color: .systemGreen)

        await store.installLocalAvatar(uploaded, jpegData: jpegData(), accountId: "gambler")

        let profile = await store.image(accountId: "gambler", targetPixelSize: 288)
        let row = await store.image(accountId: "gambler", targetPixelSize: 120)

        #expect(profile === uploaded)
        #expect(row === uploaded)
        #expect(fixture.fetcher.requestCount == 0)
        #expect(fixture.replacements.values == ["gambler"])
    }

    @Test("given a cached absence then a successful upload bypasses it immediately")
    func uploadSeedBypassesCachedAbsence() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(notFoundResponse()))
        let store = fixture.makeStore()

        let missing = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(missing == nil)

        let uploaded = uiImage(side: 512, color: .systemGreen)
        await store.installLocalAvatar(uploaded, jpegData: jpegData(), accountId: "gambler")

        let seeded = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(seeded === uploaded)
        #expect(fixture.fetcher.requestCount == 1)
    }

    @Test("given the seeded upload becomes stale then an unchanged remote object is recognized by its ETag")
    func seededEtagMatchesUnchangedRemoteObject() async {
        let fixture = Fixture()
        let data = jpegData()
        let etag = s3ETag(for: data)
        fixture.fetcher.enqueue(.success(jpegResponse(rawEtag: etag)))
        let store = fixture.makeStore()

        let uploaded = uiImage(side: 512, color: .systemGreen)
        await store.installLocalAvatar(uploaded, jpegData: data, accountId: "gambler")

        fixture.clock.now = fixture.clock.now.addingTimeInterval(6 * 60)
        let stale = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(stale === uploaded)

        await store.awaitRevalidations()
        #expect(fixture.replacements.values == ["gambler"])

        let retained = await store.image(accountId: "gambler", targetPixelSize: 128)
        #expect(retained === uploaded)
    }

    @Test("given stale URLCache bytes after an upload then the fast path rejects them and refetches")
    func uploadSeedSurvivesDecodedEvictionDespiteStaleURLCache() async {
        let fixture = Fixture()
        let newData = jpegData(color: .systemGreen)
        // URLCache still holds the pre-upload response with the old ETag.
        fixture.cachedResponses["gambler"] = jpegResponse(etag: "\"old\"", color: .systemRed)
        fixture.fetcher.enqueue(.success(jpegResponse(rawEtag: s3ETag(for: newData), data: newData)))
        let store = fixture.makeStore()

        await store.installLocalAvatar(uiImage(side: 512, color: .systemGreen), jpegData: newData, accountId: "gambler")
        await store.evictAllDecodedImages()

        let recovered = await store.image(accountId: "gambler", targetPixelSize: 128)

        // The mismatched cached bytes were rejected; the network path served the current object.
        #expect(recovered != nil)
        #expect(fixture.fetcher.requestCount == 1)
    }

    @Test("given a transient server error then no absence is cached and the next request retries")
    func serverErrorDoesNotCacheAbsence() async {
        let fixture = Fixture()
        fixture.fetcher.enqueue(.success(serverErrorResponse()))
        fixture.fetcher.enqueue(.success(jpegResponse(etag: "\"a\"")))
        let store = fixture.makeStore()

        let first = await store.image(accountId: "gambler", targetPixelSize: 128)
        let second = await store.image(accountId: "gambler", targetPixelSize: 128)

        #expect(first == nil)
        #expect(second != nil)
        #expect(fixture.fetcher.requestCount == 2)
    }

    @Test("given a pixel target then buckets round up in 32-pixel steps and cap at 512")
    func bucketsRoundUpAndCap() {
        #expect(avatarPixelBucket(forTargetPixelSize: 1) == 32)
        #expect(avatarPixelBucket(forTargetPixelSize: 32) == 32)
        #expect(avatarPixelBucket(forTargetPixelSize: 33) == 64)
        #expect(avatarPixelBucket(forTargetPixelSize: 120) == 128)
        #expect(avatarPixelBucket(forTargetPixelSize: 128) == 128)
        #expect(avatarPixelBucket(forTargetPixelSize: 511) == 512)
        #expect(avatarPixelBucket(forTargetPixelSize: 2000) == 512)
    }
}

// MARK: - Fixtures

private final class Fixture {
    let fetcher = FetchStub()
    let clock = MutableClock(Date(timeIntervalSince1970: 1_000_000))
    let replacements = ReplacementLog()
    var cachedResponses: [String: (Data, HTTPURLResponse)] = [:]

    func makeStore() -> AvatarImageStore {
        let fetcher = fetcher
        let clock = clock
        let replacements = replacements
        let cachedByAccount = { [weak self] (request: URLRequest) -> CachedURLResponse? in
            guard let self else { return nil }
            for (accountId, response) in self.cachedResponses
            where request.url?.absoluteString.contains("\(accountId).jpg") == true {
                return CachedURLResponse(response: response.1, data: response.0)
            }
            return nil
        }
        return AvatarImageStore(
            fetch: { request in try await fetcher.fetch(request) },
            cachedResponse: cachedByAccount,
            now: { clock.now },
            notifyReplacement: { accountId in replacements.append(accountId) }
        )
    }
}

private final class FetchStub: @unchecked Sendable {
    private let lock = NSLock()
    private var queue: [Result<(Data, HTTPURLResponse), Error>] = []
    private var gateContinuations: [CheckedContinuation<Void, Never>] = []
    var fallback: ((URLRequest) -> Result<(Data, HTTPURLResponse), Error>)?
    var gated = false
    private(set) var requestCount = 0

    func enqueue(_ result: Result<(Data, HTTPURLResponse), Error>) {
        lock.lock()
        queue.append(result)
        lock.unlock()
    }

    func releaseGate() {
        lock.lock()
        let continuations = gateContinuations
        gateContinuations = []
        lock.unlock()
        for continuation in continuations {
            continuation.resume()
        }
    }

    func fetch(_ request: URLRequest) async throws -> (Data, URLResponse) {
        lock.lock()
        requestCount += 1
        let shouldGate = gated
        lock.unlock()

        if shouldGate {
            await withCheckedContinuation { continuation in
                lock.lock()
                gateContinuations.append(continuation)
                lock.unlock()
            }
        }

        lock.lock()
        let result: Result<(Data, HTTPURLResponse), Error>
        if queue.isEmpty {
            result = fallback?(request) ?? .failure(URLError(.resourceUnavailable))
        } else {
            result = queue.removeFirst()
        }
        lock.unlock()

        let (data, response) = try result.get()
        return (data, response)
    }
}

private final class ReplacementLog: @unchecked Sendable {
    private let lock = NSLock()
    private var accountIds: [String] = []

    var values: [String] {
        lock.lock()
        defer { lock.unlock() }
        return accountIds
    }

    func append(_ accountId: String) {
        lock.lock()
        accountIds.append(accountId)
        lock.unlock()
    }
}

private final class MutableClock: @unchecked Sendable {
    var now: Date

    init(_ now: Date) {
        self.now = now
    }
}

// MARK: - Response helpers

private let avatarURL = URL(string: "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/gambler.jpg")!

private func uiImage(side: Int, color: UIColor = .systemRed) -> UIImage {
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    let renderer = UIGraphicsImageRenderer(size: CGSize(width: side, height: side), format: format)
    return renderer.image { context in
        color.setFill()
        context.fill(CGRect(x: 0, y: 0, width: side, height: side))
    }
}

private func jpegData(side: Int = 512, color: UIColor = .systemRed) -> Data {
    uiImage(side: side, color: color).jpegData(compressionQuality: 0.8)!
}

private func jpegResponse(etag: String, color: UIColor = .systemRed) -> (Data, HTTPURLResponse) {
    jpegResponse(rawEtag: etag, data: jpegData(color: color))
}

private func jpegResponse(rawEtag: String, data: Data = jpegData()) -> (Data, HTTPURLResponse) {
    let response = HTTPURLResponse(
        url: avatarURL,
        statusCode: 200,
        httpVersion: "HTTP/1.1",
        headerFields: ["Etag": rawEtag, "Content-Type": "image/jpeg"]
    )!
    return (data, response)
}

private func serverErrorResponse() -> (Data, HTTPURLResponse) {
    let response = HTTPURLResponse(
        url: avatarURL,
        statusCode: 503,
        httpVersion: "HTTP/1.1",
        headerFields: [:]
    )!
    return (Data(), response)
}

private func notFoundResponse() -> (Data, HTTPURLResponse) {
    let response = HTTPURLResponse(
        url: avatarURL,
        statusCode: 404,
        httpVersion: "HTTP/1.1",
        headerFields: [:]
    )!
    return (Data(), response)
}

// Matches AvatarImageStore's seeded ETag: quoted MD5 hex, the S3 ETag for a simple PUT.
private func s3ETag(for data: Data) -> String {
    "\"" + Insecure.MD5.hash(data: data).map { String(format: "%02x", $0) }.joined() + "\""
}

private func waitUntil(
    timeoutTicks: Int = 2000,
    _ condition: @escaping () -> Bool
) async {
    var ticks = 0
    while !condition() && ticks < timeoutTicks {
        await Task.yield()
        ticks += 1
    }
}
