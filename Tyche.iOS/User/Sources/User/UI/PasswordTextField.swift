import SwiftUI
import UI

struct PasswordTextField : View {
    @Binding var value : String
    @State private var isValid = true
    @State private var isPasswordTextVisible = false
    
    var body: some View {
        VStack {
            ZStack(alignment: .trailing) {
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
            
            if !isValid {
                Text(StringScheme.passwordValidationFailureMessage.localizedKey)
                    .foregroundColor(ColorScheme.errorColor)
            }
        }
    }
}

private struct InternalSecureField : View {
    @Binding var value : String
    @Binding var isValid : Bool
    @Binding var isPasswordTextVisible : Bool
    
    var body: some View {
        SecureField(StringScheme.passwordText.localizedKey, text: $value)
            .border(isValid ? Color.clear : ColorScheme.errorColor)
            .overlay(alignment: .trailing) {
                Image(systemName: isPasswordTextVisible ? "eye.slash.fill" : "eye.fill")
                    .accentColor(.secondary)
                    .padding(.horizontal)
                    .onTapGesture {
                        isPasswordTextVisible.toggle()
                    }
            }
            .onReceive(value.publisher.collect()) { newValue in
                if newValue.count > 16 {
                    value = String(newValue.prefix(16))
                }
            }
            .onChange(of: value) { newValue in
                isValid = Password.isValid(value: newValue)
            }
    }
}

private struct InternalTextField : View {
    @Binding var value : String
    @Binding var isValid : Bool
    @Binding var isPasswordTextVisible : Bool
    
    var body: some View {
        TextField(StringScheme.passwordText.localizedKey, text: $value)
            .autocapitalization(.none)
            .autocorrectionDisabled(true)
            .border(isValid ? Color.clear : ColorScheme.errorColor)
            .overlay(alignment: .trailing) {
                Image(systemName: isPasswordTextVisible ? "eye.slash.fill" : "eye.fill")
                    .accentColor(.secondary)
                    .padding(.horizontal)
                    .onTapGesture {
                        isPasswordTextVisible.toggle()
                    }
            }
            .onReceive(value.publisher.collect()) { newValue in
                if newValue.count > 16 {
                    value = String(newValue.prefix(16))
                }
            }
            .onChange(of: value) { newValue in
                isValid = Password.isValid(value: newValue)
            }
    }
}

struct PasswordTextField_Previews: PreviewProvider {
    static var previews: some View {
        @State var value = ""
        PasswordTextField(value: $value)
    }
}
