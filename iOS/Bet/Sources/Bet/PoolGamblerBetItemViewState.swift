enum PoolGamblerBetItemViewState {
    case visualization(_ value: PartialPoolGamblerBetModel)
    case edition(_ value: PartialPoolGamblerBetModel)
    
    var value: PartialPoolGamblerBetModel {
        get {
            switch self {
            case .visualization(let value): value
            case .edition(let value): value
            }
        }
        set {
            switch self {
            case .visualization: self = .visualization(newValue)
            case .edition: self = .edition(newValue)
            }
        }
    }
}

extension PoolGamblerBetItemViewState {
    func copy(value: PartialPoolGamblerBetModel) -> PoolGamblerBetItemViewState {
        switch self {
        case .visualization: .visualization(value)
        case .edition: .edition(value)
        }
    }
}

extension PoolGamblerBetItemViewState {
    static func emptyVisualization() -> PoolGamblerBetItemViewState {
        .visualization(PartialPoolGamblerBetModel.empty())
    }
}
