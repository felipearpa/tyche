struct PartialPoolGamblerBetModel {
    var homeTeamBet: String
    var awayTeamBet: String
}

extension PartialPoolGamblerBetModel {
    struct Builder {
        var homeTeamBet: String
        var awayTeamBet: String
        
        fileprivate init(original: PartialPoolGamblerBetModel) {
            self.homeTeamBet = original.homeTeamBet
            self.awayTeamBet = original.awayTeamBet
        }
        
        fileprivate func build() -> PartialPoolGamblerBetModel {
            PartialPoolGamblerBetModel(
                homeTeamBet: self.homeTeamBet,
                awayTeamBet: self.awayTeamBet
            )
        }
    }
    
    func copy(_ build: (inout Builder) -> Void) -> PartialPoolGamblerBetModel {
        var builder = Builder(original: self)
        build(&builder)
        return builder.build()
    }
}

extension PartialPoolGamblerBetModel {
    static func empty() -> PartialPoolGamblerBetModel {
        PartialPoolGamblerBetModel(homeTeamBet: "", awayTeamBet: "")
    }
}
