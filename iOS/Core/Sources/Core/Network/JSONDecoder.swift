import Foundation

public extension JSONDecoder {
    func withISODate() -> JSONDecoder {
        self.dateDecodingStrategy = .iso8601
        return self
    }
}
