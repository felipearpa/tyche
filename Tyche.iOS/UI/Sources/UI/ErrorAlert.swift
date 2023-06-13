import SwiftUI

public extension View {
    
    func errorAlert(
        _ localizedError: Binding<(some LocalizedError)?>,
        onDismiss: @escaping () -> Void) -> some View {

        let unwrappedLocalizedError = localizedError.wrappedValue
        return alert(
            isPresented: .constant(unwrappedLocalizedError != nil),
            error: unwrappedLocalizedError
        ) { _ in 
            Button(StringScheme.doneAction.localizedKey) {
                localizedError.wrappedValue = nil
                onDismiss()
            }
        } message: { localizedError in
            Text(localizedError.failureReason ?? "")
        }
    }
}
