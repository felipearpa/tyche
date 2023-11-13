import SwiftUI
import UI

struct BetTextField: View {
    @Binding var value: String
    
    var body: some View {
        let _ = Self._printChanges()
        
        TextField("", text: $value)
            .textFieldStyle(RoundedBorderTextFieldStyle())
            .keyboardType(.numberPad)
            .onChange(of: value) { newValue in
                value = newValue.normalize()
            }
    }
}

private extension String {
    func normalize() -> String {
        let numericValue = self.filter { char in char.isNumber }
        guard !numericValue.isEmpty else { return "0" }
        let nonLeadingZerosValue = String(Int(numericValue)!)
        return String(nonLeadingZerosValue.prefix(3))
    }
}

#Preview {
    BetTextField(value: .constant("0"))
}
