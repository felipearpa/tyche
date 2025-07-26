import SwiftUI

struct PoolScoreListDrawerView : View {
    @StateObject var viewModel: PoolScoreListDrawerViewModel
    let onSignOut: () -> Void

    init(
        viewModel: @autoclosure @escaping () -> PoolScoreListDrawerViewModel,
        onSignOut: @escaping () -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onSignOut = onSignOut
    }

    var body: some View {
        PoolScoreListDrawerStatefulView(onSignOut: {
            viewModel.logOut()
            onSignOut()
        })
    }
}

private struct PoolScoreListDrawerStatefulView : View {
    let onSignOut: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack {
            Spacer()

            Button(action: onSignOut) {
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
    PoolScoreListDrawerStatefulView(onSignOut: {})
}
