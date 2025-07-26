import SwiftUI
import UI
import Pool

struct PoolHomeDrawerView: View {
    @ObservedObject var viewModel: PoolHomeDrawerViewModel
    let onPoolChange: () -> Void
    let onSignOut: () -> Void

    init(viewModel: @autoclosure @escaping () -> PoolHomeDrawerViewModel,
         changePool: @escaping () -> Void,
         onLogout: @escaping () -> Void,
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onPoolChange = changePool
        self.onSignOut = onLogout
    }

    var body: some View {
        PoolHomeDrawerStatefulView(
            state: viewModel.state,
            onPoolChange: onPoolChange,
            onSignOut: {
                viewModel.signOut()
                onSignOut()
            }
        )
    }
}

private struct PoolHomeDrawerStatefulView: View {
    let state: LoadableViewState<PoolGamblerScoreModel>
    let onPoolChange: () -> Void
    let onSignOut: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack {
            switch state {
            case .failure(let error):
                FailureContent(localizedError: error.localizedErrorOrDefault())

            case .initial, .loading:
                LoadingContainerView {
                    EmptyView()
                }

            case .success(let score):
                PoolHomeDrawer(
                    currentPoolGamblerScore: score,
                    onPoolChange: onPoolChange,
                    onSignOut: { onSignOut() }
                )
                .padding(.all, boxSpacing.large)
            }
        }
        .frame(maxHeight: .infinity)
    }
}

private struct FailureContent: View {
    let localizedError: LocalizedError

    var body: some View {
        ErrorView(localizedError: localizedError)
    }
}

private struct PoolHomeDrawer: View {
    let currentPoolGamblerScore: PoolGamblerScoreModel
    let onPoolChange: () -> Void
    let onSignOut: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            VStack(spacing: boxSpacing.medium) {
                PoolSpotlightItem(poolGamblerScore: currentPoolGamblerScore)
                    .padding(.horizontal, boxSpacing.medium)

                Button(action: onPoolChange) {
                    Text(String(.changePoolAction))
                        .frame(maxWidth: .infinity, alignment: .center)
                }
                .buttonStyle(.bordered)
                .padding(.horizontal, boxSpacing.medium)

                Divider()
            }

            Spacer()

            Button(action: onSignOut) {
                HStack {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                    Text(String(.logOutAction))
                }
                .frame(maxWidth: .infinity, alignment: .center)
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal, boxSpacing.medium)
        }
    }
}

#Preview {
    PoolHomeDrawerStatefulView(
        state: .success(poolGamblerScoreDummyModel()),
        onPoolChange: {},
        onSignOut: {},
    )
}
