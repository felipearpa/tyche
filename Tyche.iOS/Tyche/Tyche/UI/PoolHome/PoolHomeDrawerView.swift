import SwiftUI
import UI
import Pool

struct PoolHomeDrawerView: View {
    @ObservedObject var viewModel: PoolHomeDrawerViewModel
    let changePool: () -> Void
    let onLogout: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    init(viewModel: @autoclosure @escaping () -> PoolHomeDrawerViewModel,
         changePool: @escaping () -> Void,
         onLogout: @escaping () -> Void,
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.changePool = changePool
        self.onLogout = onLogout
    }

    var body: some View {
        contentView(for: viewModel.state)
    }

    @ViewBuilder
    private func contentView(for state: LoadableViewState<PoolGamblerScoreModel>) -> some View {
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
                    changePool: changePool,
                    logout: {
                        viewModel.logout()
                        onLogout()
                    }
                )
                .padding(.all, boxSpacing.large)
            }
        }
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
    let changePool: () -> Void
    let logout: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: boxSpacing.medium) {
                PoolSpotlightItem(poolGamblerScore: currentPoolGamblerScore)
                    .padding(.horizontal, boxSpacing.medium)

                Button(action: changePool) {
                    Text("Change Pool") // Replace with localized string
                }
                .padding(.horizontal, boxSpacing.medium)

                Divider()
            }

            Spacer()

            Button(action: logout) {
                HStack {
                    Image(systemName: "rectangle.portrait.and.arrow.right") // Replace with appropriate asset if needed
                    Text("Log Out") // Replace with localized string
                }
            }
            .padding(.horizontal, boxSpacing.medium)
        }
    }
}
