import SwiftUI
import UI

struct BetTextField: View {
    @Binding var value: String
    var autoFocus: Bool = false
    @FocusState private var isFocused: Bool

    var body: some View {
        let _ = Self._printChangesIfDebug()

        TextField("".excludeLocalize, text: $value)
            .multilineTextAlignment(.center)
            .textFieldStyle(.liquidGlass)
            .keyboardType(.numberPad)
            .focused($isFocused)
            .onChange(of: value) { newValue in
                value = newValue.normalize()
            }
            .onAppear {
                // Anchor focus through @FocusState so it survives the list's
                // paging re-renders, and claim it on edit-entry so the user can
                // type without a second tap.
                if autoFocus { isFocused = true }
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
