import Foundation

struct PoolLayoutModel : Codable, Hashable, Identifiable, Sendable {
    let id: String
    let name: String
    let startDateTime: Date
}
