import SwiftUI
import Core
import UI
import DataPool
import ViewingState

public struct PoolFromLayoutCreatorView: View {
    @StateObject var viewModel: PoolFromLayoutCreatorViewModel
    let onPoolCreated: (String) -> Void
    let preselectedPoolLayoutId: String?
    let preselectedPoolName: String?

    public init(
        viewModel: @autoclosure @escaping () -> PoolFromLayoutCreatorViewModel,
        onPoolCreated: @escaping (String) -> Void,
        preselectedPoolLayoutId: String? = nil,
        preselectedPoolName: String? = nil,
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onPoolCreated = onPoolCreated
        self.preselectedPoolLayoutId = preselectedPoolLayoutId
        self.preselectedPoolName = preselectedPoolName
    }

    public var body: some View {
        PoolFromLayoutCreatorStatefulView(
            state: viewModel.state,
            onSaveClick: viewModel.createPool,
            onPoolCreated: onPoolCreated,
            reset: viewModel.reset,
            preselectedPoolLayoutId: preselectedPoolLayoutId,
            preselectedPoolName: preselectedPoolName,
        )
        .navigationTitle(String(localized: .poolFromLayoutCreatorTitle))
    }
}

/// One stepper, with its template list, step, and draft, serves every creation state: the loader
/// covers it while the pool is created and until the created pool is opened, and a failure leaves
/// it as it was for a retry.
struct PoolFromLayoutCreatorStatefulView: View {
    let state: MutationState<CreatePoolModel>
    let onSaveClick: (CreatePoolModel) -> Void
    let onPoolCreated: (String) -> Void
    let reset: () -> Void
    let preselectedPoolLayoutId: String?
    let preselectedPoolName: String?

    @Environment(\.diResolver) private var diResolver: DIResolver
    @Environment(\.boxSpacing) private var boxSpacing
    @State private var step: Step

    init(
        state: MutationState<CreatePoolModel>,
        onSaveClick: @escaping (CreatePoolModel) -> Void,
        onPoolCreated: @escaping (String) -> Void,
        reset: @escaping () -> Void,
        preselectedPoolLayoutId: String?,
        preselectedPoolName: String?,
    ) {
        self.state = state
        self.onSaveClick = onSaveClick
        self.onPoolCreated = onPoolCreated
        self.reset = reset
        self.preselectedPoolLayoutId = preselectedPoolLayoutId
        self.preselectedPoolName = preselectedPoolName
        self._step = State(initialValue: preselectedPoolLayoutId != nil ? .two : .one)
    }

    private var initialCreatePoolModel: CreatePoolModel {
        guard let preselectedPoolLayoutId else { return emptyCreatePoolModel() }
        return CreatePoolModel(
            poolLayoutId: preselectedPoolLayoutId,
            poolName: preselectedPoolName ?? "",
            poolId: nil,
        )
    }

    private var createdPoolId: String? {
        guard case .mutated(_, let createdPoolModel) = state else { return nil }
        return createdPoolModel.poolId
    }

    var body: some View {
        LoadingContainerView(isLoading: state.isMutating() || state.isMutated()) {
            StepperView(
                poolFromLayoutCreatorStepOneViewModel: PoolFromLayoutCreatorStepOneViewModel(
                    getOpenPoolLayoutsUseCase: diResolver.resolve(GetOpenPoolLayoutsUseCase.self)!,
                ),
                step: $step,
                onSaveClick: onSaveClick,
                initialCreatePoolModel: initialCreatePoolModel,
            )
            .padding(boxSpacing.medium)
        }
        .errorAlert(.constant(state.errorOrNil().map { $0.localizedErrorOrNil()! }), onDismiss: reset)
        // Reported on the change to a created pool, so redrawing the screen does not open it again.
        .onChange(of: createdPoolId) { poolId in
            guard let poolId else { return }
            onPoolCreated(poolId)
        }
    }
}

private struct StepperView: View {
    @StateObject var poolFromLayoutCreatorStepOneViewModel: PoolFromLayoutCreatorStepOneViewModel
    @Binding var step: Step
    let onSaveClick: (CreatePoolModel) -> Void

    @State var createPoolModel: CreatePoolModel
    @State var previousStep: Step? = nil

    init(
        poolFromLayoutCreatorStepOneViewModel: @autoclosure @escaping () -> PoolFromLayoutCreatorStepOneViewModel,
        step: Binding<Step>,
        onSaveClick: @escaping (CreatePoolModel) -> Void,
        initialCreatePoolModel: CreatePoolModel = emptyCreatePoolModel(),
    ) {
        self._poolFromLayoutCreatorStepOneViewModel = .init(wrappedValue: poolFromLayoutCreatorStepOneViewModel())
        self._step = step
        self.onSaveClick = onSaveClick
        self._createPoolModel = State(initialValue: initialCreatePoolModel)
    }

    var body: some View {
        VStack {
            switch step {
            case .one:
                PoolFromLayoutCreatorStepOneView(
                    viewModel: poolFromLayoutCreatorStepOneViewModel,
                    createPoolModel: createPoolModel,
                    onNextClick: { newCreatePoolModel in
                        createPoolModel = newCreatePoolModel
                        withAnimation { step = .two }
                    }
                )
                .transition(transition(for: .one))
            case .two:
                PoolFromLayoutCreatorStepTwoView(
                    createPoolModel: createPoolModel,
                    onSaveClick: { newCreatePoolModel in
                        createPoolModel = newCreatePoolModel
                        onSaveClick(newCreatePoolModel)
                    }
                )
                .transition(transition(for: .two))
            }
        }
        .onAppear { previousStep = step }
        .onChange(of: step) { new in previousStep = new }
        .animation(.easeInOut(duration: 0.28), value: step)
    }

    private var forwardTransition: AnyTransition {
        .asymmetric(
            insertion: .push(from: .trailing).combined(with: .opacity),
            removal:   .push(from: .leading).combined(with: .opacity)
        )
    }

    private var backwardTransition: AnyTransition {
        .asymmetric(
            insertion: .push(from: .leading).combined(with: .opacity),
            removal:   .push(from: .trailing).combined(with: .opacity)
        )
    }

    private func transition(for new: Step) -> AnyTransition {
        let isForward = (previousStep, new) == (.one, .two)
        return isForward ? forwardTransition : backwardTransition
    }
}

private enum Step {
    case one
    case two
}
