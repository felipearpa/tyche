public struct TeamScore<Value: Codable & Hashable>: Codable, Hashable {
    public let homeTeamValue: Value
    public let awayTeamValue: Value
    
    public init(homeTeamValue: Value, awayTeamValue: Value) {
        self.homeTeamValue = homeTeamValue
        self.awayTeamValue = awayTeamValue
    }
}
