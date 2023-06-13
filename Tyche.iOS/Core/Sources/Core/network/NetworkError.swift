public enum NetworkError: Error {
    case remoteCommunication
    case http(code: HTTPStatusCode)
}

public extension Result {
    
    @inlinable func mapNetworkError<NewFailure>(_ transform: (NetworkError) -> NewFailure) -> Result<Success, NewFailure> where NewFailure : Error {
        return mapError { error in
            if let networkError = error as? NetworkError {
                return transform(networkError)
            }
            return error as! NewFailure
        }
    }
}
