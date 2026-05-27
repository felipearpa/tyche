struct PoolMemberResponse: Codable {
    let gamblerId: String
    let gamblerUsername: String
    let gamblerEmail: String
    let isOwner: Bool
}

extension PoolMemberResponse {
    func toPoolMember() -> PoolMember {
        return PoolMember(
            gamblerId: self.gamblerId,
            gamblerUsername: self.gamblerUsername,
            gamblerEmail: self.gamblerEmail,
            isOwner: self.isOwner
        )
    }
}
