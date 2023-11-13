struct PoolGamblerBetItemViewState {
    var homeTeamBet: String
    var awayTeamBet: String
    fileprivate(set) var isEditable: Bool
}

extension PoolGamblerBetItemViewState {
    struct Builder {
        var homeTeamBet: String
        var awayTeamBet: String
        fileprivate var isEditable: Bool
        
        fileprivate init(original: PoolGamblerBetItemViewState) {
            self.homeTeamBet = original.homeTeamBet
            self.awayTeamBet = original.awayTeamBet
            self.isEditable = original.isEditable
        }
        
        fileprivate func build() -> PoolGamblerBetItemViewState {
            PoolGamblerBetItemViewState(
                homeTeamBet: self.homeTeamBet,
                awayTeamBet: self.awayTeamBet,
                isEditable: self.isEditable)
        }
    }
    
    func copy(_ build: (inout Builder) -> Void) -> PoolGamblerBetItemViewState {
        var builder = Builder(original: self)
        build(&builder)
        return builder.build()
    }
    
    func copyUnlocked(_ build: (inout Builder) -> Void) -> PoolGamblerBetItemViewState {
        var builder = Builder(original: self)
        build(&builder)
        builder.isEditable = true
        return builder.build()
    }
    
    func copyLocked(_ build: (inout Builder) -> Void) -> PoolGamblerBetItemViewState {
        var builder = Builder(original: self)
        build(&builder)
        builder.isEditable = false
        return builder.build()
    }
    
    mutating func lock() {
        self.isEditable = false
    }
    
    mutating func unlock() {
        self.isEditable = true
    }
    
    func isBetScoreEqual(toPoolGamblerBetScore poolGamblerBet: PoolGamblerBetModel) -> Bool {
        self.homeTeamBet == poolGamblerBet.homeTeamBetRawValue() && self.awayTeamBet == poolGamblerBet.awayTeamBetRawValue()
    }
    
    static func empty() -> PoolGamblerBetItemViewState {
        PoolGamblerBetItemViewState(
            homeTeamBet: "",
            awayTeamBet: "",
            isEditable: false)
    }
}
