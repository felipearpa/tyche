public struct CursorPage<Value: Codable>: Codable {
    public let items: [Value]
    public let next: String?
    
    public init(items: [Value], next: String?) {
        self.items = items
        self.next = next
    }
}

public extension CursorPage {
    @inlinable
    func map<OutValue>(_ transform: (Value) -> OutValue) -> CursorPage<OutValue> {
        return CursorPage<OutValue>(
            items: self.items.map(transform),
            next: self.next
        )
    }
}
