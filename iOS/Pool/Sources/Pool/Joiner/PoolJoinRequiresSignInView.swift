import SwiftUI
import UI

public struct PoolJoinRequiresSignInView: View {
    private let onDismiss: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    public init(onDismiss: @escaping () -> Void) {
        self.onDismiss = onDismiss
    }

    public var body: some View {
        VStack(spacing: 0) {
            Spacer()

            ErrorView(localizedError: PoolJoinRequiresSignInError.requiresSignIn)

            Spacer()

            Button(action: onDismiss) {
                Text(String(.gotItAction))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.liquidGlassProminent)
            .padding(.vertical, boxSpacing.medium)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(boxSpacing.medium)
    }
}

#Preview {
    PoolJoinRequiresSignInView(onDismiss: {})
}
