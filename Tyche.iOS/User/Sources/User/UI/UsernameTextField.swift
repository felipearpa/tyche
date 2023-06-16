import SwiftUI
import UI

struct UsernameTextField : View {
    @Binding var value : String
    @State private var isValid = true
    
    var body: some View {
        VStack {
            TextField(StringScheme.usernameText.localizedKey, text: $value)
                .autocapitalization(.none)
                .border(isValid ? Color.clear : ColorScheme.errorColor)
                .onReceive(value.publisher.collect()) { newValue in
                    if newValue.count > 16 {
                        value = String(newValue.prefix(16))
                    }
                }
                .onChange(of: value) { newValue in
                    isValid = Username.isValid(value: newValue)
                }
            
            if !isValid {
                Text(StringScheme.usernameValidationFailureMessage.localizedKey)
                    .foregroundColor(ColorScheme.errorColor)
            }
        }
    }
}

struct UsernameTextField_Previews: PreviewProvider {
    static var previews: some View {
        @State var value = ""
        UsernameTextField(value: $value)
    }
}
