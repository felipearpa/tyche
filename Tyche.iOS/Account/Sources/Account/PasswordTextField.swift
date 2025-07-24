import SwiftUI
import UI
import Session

struct RawPasswordTextField: View {
    @Binding var value : String

    var body: some View {
        PasswordTextField(value: $value, validation: nil)
    }
}

struct PasswordTextField: View {
    @Binding var value: String
    @State private var isValid = true
    @State private var isPasswordTextVisible = false
    let validation: TextFieldValidation?

    @Environment(\.boxSpacing) private var boxSpacing

    init(value: Binding<String>, validation: TextFieldValidation? = TextFieldValidation(
        isValid: Password.isValid,
        errorMessage: String(.passwordValidationFailureMessage),
    )) {
        self._value = value
        self.validation = validation
    }

    var body: some View {
        VStack(spacing: boxSpacing.small) {
            ZStack {
                if !isPasswordTextVisible {
                    InternalSecureField(
                        value: $value,
                        isValid: $isValid,
                        isPasswordTextVisible: $isPasswordTextVisible
                    )
                } else {
                    InternalTextField(
                        value: $value,
                        isValid: $isValid,
                        isPasswordTextVisible: $isPasswordTextVisible
                    )
                }
            }
            .onReceive(value.publisher.collect()) { newValue in
                if newValue.count > 16 {
                    value = String(newValue.prefix(16))
                }
            }
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

private struct InternalSecureField : View {
    @Binding var value : String
    @Binding var isValid : Bool
    @Binding var isPasswordTextVisible : Bool
    
    var body: some View {
        SecureField(String(.passwordLabel), text: $value)
            .border(isValid ? Color.clear : Color(sharedResource: .error))
            .overlay(alignment: .trailing) {
                Image(systemName: isPasswordTextVisible ? "eye.slash.fill" : "eye.fill")
                    .padding(.horizontal)
                    .onTapGesture { isPasswordTextVisible.toggle() }
            }
    }
}

private struct InternalTextField : View {
    @Binding var value : String
    @Binding var isValid : Bool
    @Binding var isPasswordTextVisible : Bool
    
    var body: some View {
        TextField(String(.passwordLabel), text: $value)
            .autocapitalization(.none)
            .autocorrectionDisabled(true)
            .border(isValid ? Color.clear : Color(sharedResource: .error))
            .overlay(alignment: .trailing) {
                Image(systemName: isPasswordTextVisible ? "eye.slash.fill" : "eye.fill")
                    .accentColor(.secondary)
                    .padding(.horizontal)
                    .onTapGesture {
                        isPasswordTextVisible.toggle()
                    }
            }
    }
}

#Preview {
    RawPasswordTextField(value: .constant(""))
}

#Preview {
    PasswordTextField(value: .constant(""))
}
