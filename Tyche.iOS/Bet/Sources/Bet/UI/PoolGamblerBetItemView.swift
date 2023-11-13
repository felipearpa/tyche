import SwiftUI
import UI
import Core

struct PoolGamblerBetItemView: View {
    @StateObject private var viewModel: PoolGamblerBetItemViewModel
    @State private var viewState = PoolGamblerBetItemViewState.empty()
    
    init(viewModel: @autoclosure @escaping () -> PoolGamblerBetItemViewModel) {
        let viewModel = viewModel()
        self._viewModel = .init(wrappedValue: viewModel)
        
        if case .initial(let poolGamblerBet) = viewModel.state {
            if let betScore = poolGamblerBet.betScore {
                self._viewState = .init(wrappedValue: PoolGamblerBetItemViewState(
                    homeTeamBet: String(betScore.homeTeamValue),
                    awayTeamBet: String(betScore.awayTeamValue),
                    isEditable: false))
            }
        }
    }
    
    var body: some View {
        VStack {
            switch viewModel.state {
            case .initial(let poolGamblerBet), .success(_, succeeded: let poolGamblerBet):
                PoolGamblerBetItem(
                    poolGamblerBet: poolGamblerBet,
                    homeTeamBet: $viewState.homeTeamBet,
                    awayTeamBet: $viewState.awayTeamBet,
                    isEditable: viewState.isEditable
                )
                DefaultActionBar(
                    viewModelState: viewModel.state,
                    viewState: $viewState,
                    bet: { betScore in viewModel.bet(betScore: betScore) },
                    reset: { viewModel.reset() })
            case .loading(_, target: let poolGamblerBet):
                PoolGamblerBetItem(poolGamblerBet: poolGamblerBet)
                LoadingActionBar(viewModelState: viewModel.state)
            case .failure(_, failed: let poolGamblerBet, _):
                PoolGamblerBetItem(poolGamblerBet: poolGamblerBet)
                FailureActionBar(
                    viewModelState: viewModel.state,
                    retryBet: { viewModel.retryBet() },
                    reset: { viewModel.reset() })
            }
        }.onReceive(viewModel.$state) { state in
            switch state {
            case .initial(let poolGamblerBet):
                viewState = viewState.copyLocked { targetViewState in
                    targetViewState.homeTeamBet = poolGamblerBet.homeTeamBetRawValue()
                    targetViewState.awayTeamBet = poolGamblerBet.awayTeamBetRawValue()
                }
            case .loading, .success, .failure:
                viewState.lock()
            }
        }
    }
}

private struct DefaultActionBar: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    @Binding var viewState: PoolGamblerBetItemViewState
    let bet: (TeamScore<Int>) -> Void
    let reset: () -> Void
    
    var body: some View {
        HStack(spacing: 8) {
            if viewState.isEditable {
                EditableDefaultActionBar(
                    viewModelState: viewModelState,
                    viewState: $viewState,
                    bet: bet,
                    reset: reset)
            } else {
                NonEditableDefaultActionBar(viewModelState: viewModelState, viewState: $viewState)
            }
        }
    }
}

private struct EditableDefaultActionBar: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    @Binding var viewState: PoolGamblerBetItemViewState
    let bet: (TeamScore<Int>) -> Void
    let reset: () -> Void
    
    var body: some View {
        let poolGamblerBet = viewModelState.value()
        
        StateIndicator(state: viewModelState, isEditable: viewState.isEditable)
        
        Spacer()
        
        Button(action: { reset() }) {
            Text(String(sharedResource: .cancelAction))
        }.buttonStyle(.bordered)
        
        Button(action: {
            bet(TeamScore(
                homeTeamValue: Int(viewState.homeTeamBet)!,
                awayTeamValue: Int(viewState.awayTeamBet)!))
        }) {
            Text(String(sharedResource: .saveAction))
        }
        .buttonStyle(.borderedProminent)
        .disabled(viewState.isBetScoreEqual(toPoolGamblerBetScore: poolGamblerBet))
    }
}

private struct NonEditableDefaultActionBar: View {
    let viewModelState: EditableViewState<PoolGamblerBetModel>
    @Binding var viewState: PoolGamblerBetItemViewState
    
    var body: some View {
        StateIndicator(state: viewModelState, isEditable: viewState.isEditable)
        
        Spacer()
        
        Button(action: { viewState.unlock() }) {
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
    let state: EditableViewState<PoolGamblerBetModel>
    let isEditable: Bool
    
    init(state: EditableViewState<PoolGamblerBetModel>, isEditable: Bool = false) {
        self.state = state
        self.isEditable = isEditable
    }
    
    var body: some View {
        if isEditable {
            Image(sharedResource: .pending)
        } else {
            switch state {
            case .failure:
                Image(sharedResource: .error)
                    .foregroundStyle(Color(sharedResource: .error))
            case .loading:
                ProgressView()
            case .initial(let poolGamblerBet), .success(_, succeeded: let poolGamblerBet):
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
    }
}
