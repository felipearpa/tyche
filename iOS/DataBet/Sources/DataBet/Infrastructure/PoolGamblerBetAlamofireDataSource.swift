import Foundation
import Alamofire
import Core
import Session

class PoolGamblerBetAlamofireDataSource: PoolGamblerBetRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    private let session: AuthenticatedSession
    
    init(urlBasePathProvider: URLBasePathProvider, session: AuthenticatedSession) {
        self.urlBasePathProvider = urlBasePathProvider
        self.session = session
    }
    
    func getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse> {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/gamblers/\(gamblerId)/bets/pending")!,
                parameters: ["next": next, "searchText": searchText]
            )
            .validate()
            .responseDecodable(
                of: CursorPage<PoolGamblerBetResponse>.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func getFinishedPoolGamblerBets(poolId: String, gamblerId: String, next: String?) async throws -> CursorPage<KickoffGroupResponse> {
        return try await getKickoffGroups(
            path: "pools/\(poolId)/gamblers/\(gamblerId)/bets/finished",
            next: next
        )
    }

    func getLivePoolGamblerBets(poolId: String, gamblerId: String, next: String?, searchText: String?) async throws -> Core.CursorPage<PoolGamblerBetResponse> {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/gamblers/\(gamblerId)/bets/live")!,
                parameters: ["next": next, "searchText": searchText]
            )
            .validate()
            .responseDecodable(
                of: CursorPage<PoolGamblerBetResponse>.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func getRankedPoolMatchGamblerBets(poolId: String, matchId: String, next: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/matches/\(matchId)/bets")!,
                parameters: rankedMatchBetsParameters(next: next)
            )
            .validate()
            .responseDecodable(
                of: CursorPage<PoolGamblerBetResponse>.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(
                        throwing: rankedMatchBetsError(statusCode: response.response?.statusCode, body: response.data)
                            ?? error
                    )
                }
            }
        }
    }

    func getPoolGamblerBet(poolId: String, gamblerId: String, matchId: String) async throws -> PoolGamblerBetResponse {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/gamblers/\(gamblerId)/matches/\(matchId)")!
            )
            .validate()
            .responseDecodable(
                of: PoolGamblerBetResponse.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let poolGamblerBetResponse):
                    continuation.resume(returning: poolGamblerBetResponse)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func getGamblerBetsTimeline(poolId: String, gamblerId: String, next: String?) async throws -> CursorPage<KickoffGroupResponse> {
        return try await getKickoffGroups(
            path: "pools/\(poolId)/gamblers/\(gamblerId)/bets/timeline",
            next: next
        )
    }

    /// One page of a grouped feed. A rejected continuation fails with
    /// `KickoffGroupFeedError.cursorRejected`; every other failure keeps the transport error.
    private func getKickoffGroups(path: String, next: String?) async throws -> CursorPage<KickoffGroupResponse> {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath(path)!,
                parameters: kickoffGroupFeedParameters(next: next)
            )
            .validate()
            .responseDecodable(
                of: CursorPage<KickoffGroupResponse>.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(
                        throwing: kickoffGroupFeedError(statusCode: response.response?.statusCode, body: response.data)
                            ?? error
                    )
                }
            }
        }
    }

    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("bets")!,
                method: .patch,
                parameters: betRequest,
                encoder: JSONParameterEncoder.default
            )
            .validate()
            .responseDecodable(
                of: PoolGamblerBetResponse.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let poolGamblerBetResponse):
                    continuation.resume(returning: poolGamblerBetResponse)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}

/// The ranked mode's query: the explicit sort, plus the continuation only when there is one, so
/// a first-page request never carries an empty or placeholder cursor.
func rankedMatchBetsParameters(next: String?) -> [String: String] {
    var parameters = ["sort": rankedMatchBetsSort]
    if let next {
        parameters["next"] = next
    }
    return parameters
}

/// The ranked error a failed response describes, or `nil` to keep the transport error.
func rankedMatchBetsError(statusCode: Int?, body: Data?) -> RankedMatchBetsError? {
    guard let statusCode, let body,
          let response = try? JSONDecoder().decode(ErrorCodeResponse.self, from: body)
    else { return nil }
    return RankedMatchBetsError.from(statusCode: statusCode, errorCode: response.error)
}

let rankedMatchBetsSort = "pool-score-desc"

/// A grouped feed's query: the continuation only when there is one, so a first-page request
/// never carries an empty cursor.
func kickoffGroupFeedParameters(next: String?) -> [String: String] {
    next.map { ["next": $0] } ?? [:]
}

/// The grouped-feed error a failed response describes, or `nil` to keep the transport error.
func kickoffGroupFeedError(statusCode: Int?, body: Data?) -> KickoffGroupFeedError? {
    guard let statusCode, let body,
          let response = try? JSONDecoder().decode(ErrorCodeResponse.self, from: body)
    else { return nil }
    return KickoffGroupFeedError.from(statusCode: statusCode, errorCode: response.error)
}
