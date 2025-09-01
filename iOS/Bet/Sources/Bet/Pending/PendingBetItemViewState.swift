enum PendingBetItemViewState {
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

extension PendingBetItemViewState {
    func copy(value: PartialPoolGamblerBetModel) -> PendingBetItemViewState {
        switch self {
        case .visualization: .visualization(value)
        case .edition: .edition(value)
        }
    }
}

extension PendingBetItemViewState {
    static func emptyVisualization() -> PendingBetItemViewState {
        .visualization(PartialPoolGamblerBetModel.empty())
    }
}
