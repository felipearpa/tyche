import SwiftUI
import Core
import UI
import DataPool

public struct PoolFromLayoutCreatorView: View {
    @StateObject var viewModel: PoolFromLayoutCreatorViewModel
    let onPoolCreated: (String) -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        viewModel: @autoclosure @escaping () -> PoolFromLayoutCreatorViewModel,
        onPoolCreated: @escaping (String) -> Void,
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onPoolCreated = onPoolCreated
    }

    public var body: some View {
        PoolFromLayoutCreatorStatefulView(
            state: viewModel.state,
            onSaveClick: viewModel.createPool,
            onPoolCreated: onPoolCreated,
            reset: viewModel.reset
        )
        .navigationTitle(String(.poolFromLayoutCreatorTitle))
        .padding(boxSpacing.medium)
    }
}

private struct PoolFromLayoutCreatorStatefulView: View {
    let state: EditableViewState<CreatePoolModel>
    let onSaveClick: (CreatePoolModel) -> Void
    let onPoolCreated: (String) -> Void
    let reset: () -> Void

    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var step: Step = .one

    var body: some View {
        switch state {
        case .initial:
            StepperView(
                poolFromLayoutCreatorStepOneViewModel: PoolFromLayoutCreatorStepOneViewModel(
                    getOpenPoolLayoutsUseCase: diResolver.resolve(GetOpenPoolLayoutsUseCase.self)!,
                ),
                step: $step,
                onSaveClick: { createPoolModel in
                    onSaveClick(createPoolModel)
                },
            )

        case .saving:
            LoadingContainerView {
                StepperView(
                    poolFromLayoutCreatorStepOneViewModel: PoolFromLayoutCreatorStepOneViewModel(
                        getOpenPoolLayoutsUseCase: diResolver.resolve(GetOpenPoolLayoutsUseCase.self)!,
                    ),
                    step: $step,
                    onSaveClick: { createPoolModel in },
                )
            }

        case .success(_, let succededCreatePoolModel):
            LoadingContainerView {
                StepperView(
                    poolFromLayoutCreatorStepOneViewModel: PoolFromLayoutCreatorStepOneViewModel(
                        getOpenPoolLayoutsUseCase: diResolver.resolve(GetOpenPoolLayoutsUseCase.self)!,
                    ),
                    step: $step,
                    onSaveClick: { createPoolModel in
                        onSaveClick(createPoolModel)
                    },
                )
            }.onAppear {
                guard let poolId = succededCreatePoolModel.poolId else { return }
                onPoolCreated(poolId)
            }

        case .failure( _, _, let error):
            StepperView(
                poolFromLayoutCreatorStepOneViewModel: PoolFromLayoutCreatorStepOneViewModel(
                    getOpenPoolLayoutsUseCase: diResolver.resolve(GetOpenPoolLayoutsUseCase.self)!,
                ),
                step: $step,
                onSaveClick: { createPoolModel in
                    onSaveClick(createPoolModel)
                },
            )
            .errorAlert(.constant(error.localizedErrorOrNil()!), onDismiss: reset)
        }
    }
}

private struct StepperView: View {
    @StateObject var poolFromLayoutCreatorStepOneViewModel: PoolFromLayoutCreatorStepOneViewModel
    @Binding var step: Step
    let onSaveClick: (CreatePoolModel) -> Void

    @State var createPoolModel: CreatePoolModel = emptyCreatePoolModel()
    @State var previousStep: Step? = nil

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
