import SwiftUI
import UI
import Core
import ViewingState

struct PendingBetItemView: View {
    @StateObject private var viewModel: PendingBetItemViewModel
    @State private var viewState = PendingBetItemViewState.emptyVisualization()
    let poolGamblerBet: PoolGamblerBetModel

    init(
        viewModel: @autoclosure @escaping () -> PendingBetItemViewModel,
        poolGamblerBet: PoolGamblerBetModel
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.poolGamblerBet = poolGamblerBet
    }

    var body: some View {
        StatefulPendingBetItemView(
            viewModelState: viewModel.state ?? .idle(poolGamblerBet),
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
            reset: {
                viewState = .visualization(viewState.value)
                viewModel.reset()
            },
            edit: { viewState = .edition(viewState.value) }
        )
        .task(id: poolGamblerBet) { viewModel.bind(poolGamblerBet) }
        .onReceive(viewModel.$state) { state in
            guard let state = state else { return }
            let poolGamblerBet = state.activeValue()
            viewState = .visualization(poolGamblerBet.toPartialPoolGamblerBet())
        }
    }
}

private struct StatefulPendingBetItemView: View {
    let viewModelState: MutationState<PoolGamblerBetModel>
    @Binding var viewState: PendingBetItemViewState
    let bet: () -> Void
    let retryBet: () -> Void
    let reset: () -> Void
    let edit: () -> Void
    
    init(
        viewModelState: MutationState<PoolGamblerBetModel>,
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
            case .idle(let poolGamblerBet), .mutated(_, updated: let poolGamblerBet):
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
            case .mutating(_, updated: let poolGamblerBet):
                PendingBetItem(
                    poolGamblerBet: poolGamblerBet,
                    viewState: .constant(.visualization(poolGamblerBet.toPartialPoolGamblerBet()))
                )
                LoadingActionBar(viewModelState: viewModelState)
            case .failure(_, updated: let poolGamblerBet, _):
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
    let viewModelState: MutationState<PoolGamblerBetModel>
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
    let viewModelState: MutationState<PoolGamblerBetModel>
    @Binding var viewState: PendingBetItemViewState
    let bet: () -> Void
    let reset: () -> Void
    
    var body: some View {
        StateIndicator(state: viewModelState, isEditable: true)
        
        Spacer()
        
        Button(action: reset) {
            Text(sharedResource: .cancelAction)
        }.buttonStyle(.liquidGlass)

        Button(action: bet) {
            Text(sharedResource: .saveAction)
        }
        .buttonStyle(.liquidGlassProminent)
    }
}

private struct NonEditableDefaultActionBar: View {
    let viewModelState: MutationState<PoolGamblerBetModel>
    @Binding var viewState: PendingBetItemViewState
    let edit: () -> Void

    var body: some View {
        StateIndicator(state: viewModelState)

        Spacer()

        if !viewModelState.activeValue().isLocked {
            Button(action: edit) {
                Text(sharedResource: .editAction)
            }
        }
    }
}

private struct LoadingActionBar: View {
    let viewModelState: MutationState<PoolGamblerBetModel>

    var body: some View {
        HStack(spacing: 8) {
            StateIndicator(state: viewModelState)
            Spacer()
        }
    }
}

private struct FailureActionBar: View {
    let viewModelState: MutationState<PoolGamblerBetModel>
    let retryBet: () -> Void
    let reset: () -> Void
    
    var body: some View {
        HStack(spacing: 8) {
            StateIndicator(state: viewModelState)
            
            Spacer()
            
            Button(action: { reset() }) {
                Text(sharedResource: .cancelAction)
            }
            .buttonStyle(.liquidGlass)
            .tint(Color(sharedResource: .error))

            Button(action: { retryBet() }) {
                Text(sharedResource: .retryAction)
            }
            .buttonStyle(.liquidGlassProminent)
            .tint(Color(sharedResource: .error))
        }
    }
}

private struct StateIndicator: View {
    let viewModelstate: MutationState<PoolGamblerBetModel>
    let isEditable: Bool
    
    init(state: MutationState<PoolGamblerBetModel>, isEditable: Bool = false) {
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
            case .mutating:
                BallSpinner()
                    .frame(width: ICON_SIZE, height: ICON_SIZE)
            case .idle(let poolGamblerBet), .mutated(_, updated: let poolGamblerBet):
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
        viewModelState: .idle(poolGamblerBetDummyModel()),
        viewState: .constant(.visualization(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Editable Initial") {
    StatefulPendingBetItemView(
        viewModelState: .idle(poolGamblerBetDummyModel()),
        viewState: .constant(.edition(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Non Editable Loading") {
    StatefulPendingBetItemView(
        viewModelState: .mutating(
            original: poolGamblerBetDummyModel(),
            updated: poolGamblerBetDummyModel()
        ),
        viewState: .constant(.visualization(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Editable Loading") {
    StatefulPendingBetItemView(
        viewModelState: .mutating(
            original: poolGamblerBetDummyModel(),
            updated: poolGamblerBetDummyModel()
        ),
        viewState: .constant(.edition(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Non Editable Failure") {
    StatefulPendingBetItemView(
        viewModelState: .failure(
            original: poolGamblerBetDummyModel(),
            updated: poolGamblerBetDummyModel(),
            error: UnknownLocalizedError()
        ),
        viewState: .constant(.visualization(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Editable Failure") {
    StatefulPendingBetItemView(
        viewModelState: .failure(
            original: poolGamblerBetDummyModel(),
            updated: poolGamblerBetDummyModel(),
            error: UnknownLocalizedError()
        ),
        viewState: .constant(.edition(partialPoolGamblerBetDummyModel()))
    )
}
