import Alamofire
import Foundation
import Testing
@testable import Session

@Suite("AuthInterceptor")
struct AuthInterceptorTests {

    @Test("given a 401 when the request runs then one forced refresh happens and the request is retried once with the new token")
    func retriesOnceWithRefreshedToken() async {
        let retriever = FakeAuthTokenRetriever(token: "expired", forcedToken: "refreshed")
        let server = StubServer(statusCodes: [401, 200])

        let result = await send(through: server, retriever: retriever)

        #expect(result.isSuccess)
        #expect(server.receivedAuthorizations == ["Bearer expired", "Bearer refreshed"])
        #expect(retriever.forcedRefreshCount == 1)
    }

    @Test("given the retried request also gets a 401 when the request runs then it is not retried again and the 401 surfaces")
    func doesNotRetryASecondTime() async {
        let retriever = FakeAuthTokenRetriever(token: "expired", forcedToken: "refreshed")
        let server = StubServer(statusCodes: [401, 401, 200])

        let result = await send(through: server, retriever: retriever)

        #expect(result.unacceptableStatusCode == 401)
        #expect(server.receivedAuthorizations == ["Bearer expired", "Bearer refreshed"])
        #expect(retriever.forcedRefreshCount == 1)
    }

    @Test("given the forced refresh fails when a request gets a 401 then it is not retried and the 401 surfaces")
    func doesNotRetryWhenRefreshFails() async {
        let retriever = FakeAuthTokenRetriever(token: "expired", forcedRefreshError: TestError())
        let server = StubServer(statusCodes: [401, 200])

        let result = await send(through: server, retriever: retriever)

        #expect(result.unacceptableStatusCode == 401)
        #expect(server.receivedAuthorizations == ["Bearer expired"])
        // Alamofire consults the retrier again when the response serializer fails, so a failed
        // refresh can be attempted once more; the request itself is still sent only once.
        #expect(retriever.forcedRefreshCount > 0)
    }

    @Test("given the forced refresh yields no token when a request gets a 401 then it is not retried and the 401 surfaces")
    func doesNotRetryWhenRefreshYieldsNoToken() async {
        let retriever = FakeAuthTokenRetriever(token: "expired", forcedToken: nil)
        let server = StubServer(statusCodes: [401, 200])

        let result = await send(through: server, retriever: retriever)

        #expect(result.unacceptableStatusCode == 401)
        #expect(server.receivedAuthorizations == ["Bearer expired"])
        // Alamofire consults the retrier again when the response serializer fails, so a failed
        // refresh can be attempted once more; the request itself is still sent only once.
        #expect(retriever.forcedRefreshCount > 0)
    }

    @Test("given a non-401 error when the request runs then it is not retried and nothing is refreshed", arguments: [403, 500])
    func doesNotRetryOtherErrors(statusCode: Int) async {
        let retriever = FakeAuthTokenRetriever(token: "valid", forcedToken: "refreshed")
        let server = StubServer(statusCodes: [statusCode, 200])

        let result = await send(through: server, retriever: retriever)

        #expect(result.unacceptableStatusCode == statusCode)
        #expect(server.receivedAuthorizations == ["Bearer valid"])
        #expect(retriever.forcedRefreshCount == 0)
    }

    @Test("given several requests get a 401 at the same time when they run then one forced refresh serves all their retries")
    func coalescesConcurrentRefreshes() async {
        let requestCount = 4
        let server = StubServer(statusCodes: Array(repeating: 401, count: requestCount) + Array(repeating: 200, count: requestCount))
        let retriever = FakeAuthTokenRetriever(
            token: "expired",
            forcedToken: "refreshed",
            beforeForcedRefresh: { await server.waitUntilServed(requestCount) }
        )
        let session = makeSession(retriever: retriever)

        let results = await withTaskGroup(of: Result<Data, AFError>.self) { group in
            for _ in 0..<requestCount {
                group.addTask { await session.request(server.url).validate().serializingData().result }
            }
            return await group.reduce(into: []) { $0.append($1) }
        }

        #expect(results.allSatisfy { $0.isSuccess })
        #expect(retriever.forcedRefreshCount == 1)
        #expect(server.receivedAuthorizations.filter { $0 == "Bearer refreshed" }.count == requestCount)
    }

    @Test("given a forced refresh already finished when a later request gets a 401 then a new forced refresh happens")
    func doesNotReuseAFinishedRefresh() async {
        let retriever = FakeAuthTokenRetriever(token: "expired", forcedToken: "refreshed")
        let server = StubServer(statusCodes: [401, 200, 401, 200])
        let session = makeSession(retriever: retriever)

        let first = await session.request(server.url).validate().serializingData().result
        let second = await session.request(server.url).validate().serializingData().result

        #expect(first.isSuccess)
        #expect(second.isSuccess)
        #expect(retriever.forcedRefreshCount == 2)
    }

    @Test("given fetching the token fails when the request runs then it fails with that error and is never sent")
    func failsWhenTokenFetchFails() async {
        let retriever = FakeAuthTokenRetriever(tokenError: TestError())
        let server = StubServer(statusCodes: [200])

        let result = await send(through: server, retriever: retriever)

        guard case .failure(.requestAdaptationFailed(let error)) = result else {
            Issue.record("expected an adaptation failure, got \(result)")
            return
        }
        #expect(error is TestError)
        #expect(server.receivedAuthorizations.isEmpty)
        #expect(retriever.forcedRefreshCount == 0)
    }

    @Test("given no signed-in user when the request runs then it is sent without an Authorization header")
    func sendsWithoutHeaderWhenSignedOut() async {
        let retriever = FakeAuthTokenRetriever(token: nil)
        let server = StubServer(statusCodes: [200])

        let result = await send(through: server, retriever: retriever)

        #expect(result.isSuccess)
        #expect(server.receivedAuthorizations == [nil])
    }
}

private func makeSession(retriever: AuthTokenRetriever) -> Alamofire.Session {
    let configuration = URLSessionConfiguration.ephemeral
    configuration.protocolClasses = [StubURLProtocol.self]
    return Alamofire.Session(
        configuration: configuration,
        interceptor: AuthInterceptor(authTokenRetriever: retriever)
    )
}

private func send(through server: StubServer, retriever: AuthTokenRetriever) async -> Result<Data, AFError> {
    // The session must outlive the request: deallocating an Alamofire session cancels its requests.
    let session = makeSession(retriever: retriever)
    let result = await session.request(server.url).validate().serializingData().result
    withExtendedLifetime(session) {}
    return result
}

private extension Result {
    var isSuccess: Bool {
        if case .success = self { return true }
        return false
    }
}

private extension Result where Failure == AFError {
    var unacceptableStatusCode: Int? {
        guard case .failure(.responseValidationFailed(.unacceptableStatusCode(let code))) = self else { return nil }
        return code
    }
}

private struct TestError: Error {}

private final class FakeAuthTokenRetriever: AuthTokenRetriever, @unchecked Sendable {
    private let lock = NSLock()
    private let token: Result<String?, TestError>
    private let forcedToken: Result<String?, TestError>
    private let beforeForcedRefresh: @Sendable () async -> Void
    private var hasRefreshed = false
    private var storedForcedRefreshCount = 0

    init(
        token: String? = nil,
        tokenError: TestError? = nil,
        forcedToken: String? = nil,
        forcedRefreshError: TestError? = nil,
        beforeForcedRefresh: @escaping @Sendable () async -> Void = {}
    ) {
        self.token = tokenError.map { .failure($0) } ?? .success(token)
        self.forcedToken = forcedRefreshError.map { .failure($0) } ?? .success(forcedToken)
        self.beforeForcedRefresh = beforeForcedRefresh
    }

    var forcedRefreshCount: Int {
        lock.withLock { storedForcedRefreshCount }
    }

    func authToken(forceRefresh: Bool) async throws -> String? {
        guard forceRefresh else {
            // Like Firebase, a non-forced fetch returns the latest token once a refresh succeeded.
            let refreshed = lock.withLock { hasRefreshed }
            return refreshed ? try forcedToken.get() : try token.get()
        }

        lock.withLock { storedForcedRefreshCount += 1 }
        await beforeForcedRefresh()
        let refreshedToken = try forcedToken.get()
        lock.withLock { hasRefreshed = refreshedToken != nil }
        return refreshedToken
    }
}

/// Answers requests to its own host with the scripted status codes, in order, and records the
/// Authorization header of every request it receives.
private final class StubServer: @unchecked Sendable {
    let url: URL
    private let lock = NSLock()
    private var statusCodes: [Int]
    private var storedReceivedAuthorizations: [String?] = []

    init(statusCodes: [Int]) {
        self.url = URL(string: "https://\(UUID().uuidString.lowercased()).stub.test/resource")!
        self.statusCodes = statusCodes
        StubURLProtocol.register(self, for: url.host!)
    }

    var receivedAuthorizations: [String?] {
        lock.withLock { storedReceivedAuthorizations }
    }

    func respond(to request: URLRequest) -> Int {
        lock.withLock {
            storedReceivedAuthorizations.append(request.value(forHTTPHeaderField: "Authorization"))
            return statusCodes.removeFirst()
        }
    }

    func waitUntilServed(_ count: Int) async {
        while receivedAuthorizations.count < count {
            try? await Task.sleep(nanoseconds: 10_000_000)
        }
        // Gives the last 401s time to reach the retrier before the refresh completes.
        try? await Task.sleep(nanoseconds: 200_000_000)
    }
}

private final class StubURLProtocol: URLProtocol {
    private static let lock = NSLock()
    nonisolated(unsafe) private static var servers: [String: StubServer] = [:]

    static func register(_ server: StubServer, for host: String) {
        lock.withLock { servers[host] = server }
    }

    private static func server(for host: String?) -> StubServer? {
        lock.withLock { host.flatMap { servers[$0] } }
    }

    override class func canInit(with request: URLRequest) -> Bool {
        server(for: request.url?.host) != nil
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let url = request.url, let server = Self.server(for: url.host) else {
            client?.urlProtocol(self, didFailWithError: URLError(.unsupportedURL))
            return
        }
        let statusCode = server.respond(to: request)
        let response = HTTPURLResponse(url: url, statusCode: statusCode, httpVersion: "HTTP/1.1", headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data("{}".utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
