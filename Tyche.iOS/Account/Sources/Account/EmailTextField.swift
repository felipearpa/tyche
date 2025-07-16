import SwiftUI
import Core
import UI
import Session

struct EmailTextField : View {
    @Binding var value : String
    
    @Environment(\.boxSpacing) var boxSpacing
    @State private var isValid = true
    
    var body: some View {
        VStack(spacing: boxSpacing.small) {
            TextField(String(.emailText), text: $value)
                .autocapitalization(.none)
                .border(isValid ? Color.clear : Color(sharedResource: .error))
                .onChange(of: value) { newValue in
                    withAnimation {
                        isValid = Email.isValid(newValue)
                    }
                }
            
            if !isValid {
                Text(String(.emailValidationFailureMessage))
                    .foregroundColor(Color(sharedResource: .error))
                    .font(.caption)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .transition(
            .move(edge: .top)
            .combined(with: .opacity))
        .onAppear {
            isValid = value == "" || Email.isValid(value)
        }
    }
}

#Preview("Valid") {
    EmailTextField(value: .constant("tyche@tyche.com"))
}

#Preview("Invalid") {
    EmailTextField(value: .constant("invalid"))
}
