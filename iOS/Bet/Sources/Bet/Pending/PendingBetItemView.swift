import SwiftUI
import UI
import Core

struct PendingBetItemView: View {
    @StateObject private var viewModel: PendingBetItemViewModel
    @State private var viewState = PendingBetItemViewState.emptyVisualization()
    
    init(viewModel: @autoclosure @escaping () -> PendingBetItemViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    var body: some View {
        StatefulPendingBetItemView(
            viewModelState: viewModel.state,
            viewState: $viewState,
            bet: {
                viewModel.bet(
                    betScore: TeamScore(
                        homeTeamValue: Int(viewState.value.homeTeamBet)!,
                        awayTeamValue: Int(viewState.value.awayTeamBet)!
                    )
                )
            },
            retryBet: viewModel.retryBet,
            reset: viewModel.reset,
            edit: { viewState = .edition(viewState.value) }
        ).onReceive(viewModel.$state) { state in
            viewState = switch state {
            case .initial(let poolGamblerBet):
                viewState.copy(
                    value: PartialPoolGamblerBetModel(
                        homeTeamBet: poolGamblerBet.homeTeamBetRawValue(),
                        awayTeamBet: poolGamblerBet.awayTeamBetRawValue()
                    )
                )
            default: .visualization(viewState.value)
            }
        }
    }
}

private struct StatefulPendingBetItemView: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    @Binding var viewState: PendingBetItemViewState
    let bet: () -> Void
    let retryBet: () -> Void
    let reset: () -> Void
    let edit: () -> Void
    
    init(
        viewModelState: EditableViewState<PoolGamblerBetModel>,
        viewState: Binding<PendingBetItemViewState>,
        bet: @escaping () -> Void = {},
        retryBet: @escaping () -> Void = {},
        reset: @escaping () -> Void = {},
        edit: @escaping () -> Void = {}
    ) {
        self.viewModelState = viewModelState
        self._viewState = viewState
        self.bet = bet
        self.retryBet = retryBet
        self.reset = reset
        self.edit = edit
    }
    
    var body: some View {
        VStack {
            switch viewModelState {
            case .initial(let poolGamblerBet), .success(_, succeeded: let poolGamblerBet):
                PendingBetItem(
                    poolGamblerBet: poolGamblerBet,
                    viewState: $viewState
                )
                DefaultActionBar(
                    viewModelState: viewModelState,
                    viewState: $viewState,
                    bet: bet,
                    reset: reset,
                    edit: edit
                )
            case .saving(_, target: let poolGamblerBet):
                PendingBetItem(
                    poolGamblerBet: poolGamblerBet,
                    viewState: .constant(.visualization(viewState.value))
                )
                LoadingActionBar(viewModelState: viewModelState)
            case .failure(_, failed: let poolGamblerBet, _):
                PendingBetItem(
                    poolGamblerBet: poolGamblerBet,
                    viewState: .constant(.visualization(viewState.value))
                )
                FailureActionBar(
                    viewModelState: viewModelState,
                    retryBet: retryBet,
                    reset: reset
                )
            }
        }
    }
}

private struct DefaultActionBar: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    @Binding var viewState: PendingBetItemViewState
    let bet: () -> Void
    let reset: () -> Void
    let edit: () -> Void
    
    var body: some View {
        HStack(spacing: 8) {
            switch viewState {
            case .visualization:
                NonEditableDefaultActionBar(
                    viewModelState: viewModelState,
                    viewState: $viewState,
                    edit: edit
                )
            case .edition:
                EditableDefaultActionBar(
                    viewModelState: viewModelState,
                    viewState: $viewState,
                    bet: bet,
                    reset: reset
                )
            }
        }
    }
}

private struct EditableDefaultActionBar: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    @Binding var viewState: PendingBetItemViewState
    let bet: () -> Void
    let reset: () -> Void
    
    var body: some View {
        StateIndicator(state: viewModelState, isEditable: true)
        
        Spacer()
        
        Button(action: reset) {
            Text(String(sharedResource: .cancelAction))
        }.buttonStyle(.bordered)
        
        Button(action: bet) {
            Text(String(sharedResource: .saveAction))
        }
        .buttonStyle(.borderedProminent)
    }
}

private struct NonEditableDefaultActionBar: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    @Binding var viewState: PendingBetItemViewState
    let edit: () -> Void
    
    var body: some View {
        StateIndicator(state: viewModelState)
        
        Spacer()
        
        Button(action: edit) {
            Text(String(sharedResource: .editAction))
        }
    }
}

private struct LoadingActionBar: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    
    var body: some View {
        HStack(spacing: 8) {
            StateIndicator(state: viewModelState)
            Spacer()
        }
    }
}

private struct FailureActionBar: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    let retryBet: () -> Void
    let reset: () -> Void
    
    var body: some View {
        HStack(spacing: 8) {
            StateIndicator(state: viewModelState)
            
            Spacer()
            
            Button(action: { reset() }) {
                Text(String(sharedResource: .cancelAction))
            }
            .buttonStyle(.bordered)
            .tint(Color(sharedResource: .error))
            
            Button(action: { retryBet() }) {
                Text(String(sharedResource: .retryAction))
            }
            .buttonStyle(.borderedProminent)
            .tint(Color(sharedResource: .error))
        }
    }
}

private struct StateIndicator: View {
    let viewModelstate: EditableViewState<PoolGamblerBetModel>
    let isEditable: Bool
    
    init(state: EditableViewState<PoolGamblerBetModel>, isEditable: Bool = false) {
        self.viewModelstate = state
        self.isEditable = isEditable
    }
    
    var body: some View {
        if isEditable {
            Image(sharedResource: .pending)
        } else {
            switch viewModelstate {
            case .failure:
                Image(sharedResource: .error)
                    .foregroundStyle(Color(sharedResource: .error))
            case .saving:
                BallSpinner()
                    .frame(width: ICON_SIZE, height: ICON_SIZE)
            case .initial(let poolGamblerBet), .success(_, succeeded: let poolGamblerBet):
                ContentIndicator(poolGamblerBet: poolGamblerBet)
            }
        }
    }
}

private struct ContentIndicator: View {
    let poolGamblerBet: PoolGamblerBetModel
    
    var body: some View {
        if poolGamblerBet.isLocked {
            Image(sharedResource: .lock)
        } else {
            if poolGamblerBet.betScore == nil {
                Image(sharedResource: .pending)
            } else {
                Image(sharedResource: .done)
            }
        }
    }
}

private let ICON_SIZE: CGFloat = 24

#Preview("Non Editable Initial") {
    StatefulPendingBetItemView(
        viewModelState: .initial(poolGamblerBetDummyModel()),
        viewState: .constant(.visualization(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Editable Initial") {
    StatefulPendingBetItemView(
        viewModelState: .initial(poolGamblerBetDummyModel()),
        viewState: .constant(.edition(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Non Editable Loading") {
    StatefulPendingBetItemView(
        viewModelState: .saving(
            current: poolGamblerBetDummyModel(),
            target: poolGamblerBetDummyModel()
        ),
        viewState: .constant(.visualization(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Editable Loading") {
    StatefulPendingBetItemView(
        viewModelState: .saving(
            current: poolGamblerBetDummyModel(),
            target: poolGamblerBetDummyModel()
        ),
        viewState: .constant(.edition(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Non Editable Failure") {
    StatefulPendingBetItemView(
        viewModelState: .failure(
            current: poolGamblerBetDummyModel(),
            failed: poolGamblerBetDummyModel(),
            error: UnknownLocalizedError()
        ),
        viewState: .constant(.visualization(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Editable Failure") {
    StatefulPendingBetItemView(
        viewModelState: .failure(
            current: poolGamblerBetDummyModel(),
            failed: poolGamblerBetDummyModel(),
            error: UnknownLocalizedError()
        ),
        viewState: .constant(.edition(partialPoolGamblerBetDummyModel()))
    )
}
