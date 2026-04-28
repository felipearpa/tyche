import SwiftUI
import UI

public struct PoolJoinRequiresSignInView: View {
    private let onDismiss: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    public init(onDismiss: @escaping () -> Void) {
        self.onDismiss = onDismiss
    }

    public var body: some View {
        VStack(spacing: boxSpacing.large) {
            ErrorView(localizedError: PoolJoinRequiresSignInError.requiresSignIn)

            Button(action: onDismiss) {
                Text(String(.gotItAction))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(boxSpacing.medium)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
    }
}

#Preview {
    PoolJoinRequiresSignInView(onDismiss: {})
}
