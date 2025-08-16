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

    var body: some View {
        switch state {
        case .initial:
            StepperView(
                onSaveClick: { createPoolModel in
                    onSaveClick(createPoolModel)
                },
            )

        case .saving:
            LoadingContainerView {
                StepperView(
                    onSaveClick: { createPoolModel in
                        onSaveClick(createPoolModel)
                    },
                )
            }

        case .success(_, let succededCreatePoolModel):
            LoadingContainerView {
                StepperView(
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
                onSaveClick: { createPoolModel in
                    onSaveClick(createPoolModel)
                },
            )
            .errorAlert(.constant(error.localizedErrorOrNil()!), onDismiss: reset)
        }
    }
}

private struct StepperView: View {
    let onSaveClick: (CreatePoolModel) -> Void

    @State private var step: Step = .one
    @State private var isForward: Bool = true
    @State var createPoolModel: CreatePoolModel = emptyCreatePoolModel()
    @Environment(\.diResolver) private var diResolver: DIResolver

    var body: some View {
        VStack {
            switch step {
            case .one:
                PoolFromLayoutCreatorStepOneView(
                    viewModel: PoolFromLayoutCreatorStepOneViewModel(
                        getOpenPoolLayoutsUseCase: diResolver.resolve(GetOpenPoolLayoutsUseCase.self)!,
                    ),
                    createPoolModel: createPoolModel,
                    onNextClick: { newCreatePoolModel in
                        createPoolModel = newCreatePoolModel
                        withAnimation {
                            isForward = true
                            step = .two
                        }
                    }
                )
                .transition(isForward ? .move(edge: .leading) : .move(edge: .trailing))
            case .two:
                PoolFromLayoutCreatorStepTwoView(
                    createPoolModel: createPoolModel,
                    onSaveClick: { newCreatePoolModel in
                        createPoolModel = newCreatePoolModel
                        onSaveClick(newCreatePoolModel)
                    }
                )
                .transition(isForward ? .move(edge: .trailing) : .move(edge: .leading))
            }
        }
        .animation(.easeInOut, value: step)
    }
}

private enum Step {
    case one
    case two
}
