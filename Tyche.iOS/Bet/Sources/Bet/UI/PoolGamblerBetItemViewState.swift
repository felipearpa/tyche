struct PoolGamblerBetItemViewState {
    var homeTeamBet: String
    var awayTeamBet: String
    var isEditable: Bool
}

extension PoolGamblerBetItemViewState {
    struct Builder {
        var homeTeamBet: String
        var awayTeamBet: String
        var isEditable: Bool
        
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
