import SwiftUI

struct PoolScoreListDrawerView : View {
    @StateObject var viewModel: PoolScoreListDrawerViewModel
    let onLogOut: () -> Void

    init(
        viewModel: @autoclosure @escaping () -> PoolScoreListDrawerViewModel,
        onLogOut: @escaping () -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onLogOut = onLogOut
    }

    var body: some View {
        PoolScoreListDrawerStateView(onLogOutClick: {
            viewModel.logOut()
            onLogOut()
        })
    }
}

private struct PoolScoreListDrawerStateView : View {
    let onLogOutClick: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack {
            Spacer()

            Button(action: onLogOutClick) {
                HStack(spacing: boxSpacing.small) {
                    Image(.logOut)
                    Text(String(.logOutAction))
                }
                .frame(maxWidth: .infinity, alignment: .center)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(boxSpacing.medium)
    }
}

#Preview {
    PoolScoreListDrawerStateView(onLogOutClick: {})
}
