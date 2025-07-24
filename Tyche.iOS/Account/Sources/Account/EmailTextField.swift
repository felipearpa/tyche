import SwiftUI
import Core
import UI
import Session

struct RawEmailTextField : View {
    @Binding var value : String

    var body: some View {
        EmailTextField(value: $value, validation: nil)
    }
}

struct EmailTextField : View {
    @Binding var value : String
    let validation: TextFieldValidation?

    @Environment(\.boxSpacing) var boxSpacing
    @State private var isValid = true

    init(
        value: Binding<String>,
        validation: TextFieldValidation? = TextFieldValidation(
            isValid: Email.isValid,
            errorMessage: String(.emailValidationFailureMessage)
        )
    ) {
        self._value = value
        self.validation = validation
    }

    var body: some View {
        VStack(spacing: boxSpacing.small) {
            TextField(String(.emailLabel), text: $value)
                .autocapitalization(.none)
                .border(isValid ? Color.clear : Color(sharedResource: .error))
                .onChange(of: value) { newValue in
                    withAnimation {
                        isValid = validation?.isValid(newValue) ?? true
                    }
                }

            if !isValid {
                Text(validation?.errorMessage ?? "")
                    .foregroundColor(Color(sharedResource: .error))
                    .font(.caption)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .transition(
            .move(edge: .top)
            .combined(with: .opacity)
        )
    }
}

#Preview {
    RawEmailTextField(value: .constant(""))
}

#Preview {
    EmailTextField(value: .constant(""))
}
