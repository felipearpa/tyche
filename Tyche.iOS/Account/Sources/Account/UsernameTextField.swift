import SwiftUI
import UI
import Session

struct UsernameTextField : View {
    @Binding var value : String
    @State private var isValid = true
    
    var body: some View {
        VStack {
            TextField(String(.usernameText), text: $value)
                .autocapitalization(.none)
                .border(isValid ? Color.clear : Color(sharedResource: .error))
                .onReceive(value.publisher.collect()) { newValue in
                    if newValue.count > 16 {
                        value = String(newValue.prefix(16))
                    }
                }
                .onChange(of: value) { newValue in
                    isValid = Username.isValid(value: newValue)
                }
            
            if !isValid {
                Text(String(.usernameValidationFailureMessage))
                    .foregroundColor(Color(sharedResource: .error))
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
