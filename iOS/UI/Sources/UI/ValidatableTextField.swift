import SwiftUI

public struct ValidatableTextField: View {
    private let label: String
    @Binding private var value : String
    private let isValid: Bool
    private let errorMessage: String?

    @FocusState private var wasTouched: Bool
    @Environment(\.boxSpacing) var boxSpacing

    public init(
        label: String,
        value: Binding<String>,
        isValid: Bool,
        errorMessage: String?,
    ) {
        self.label = label
        self._value = value
        self.isValid = isValid
        self.errorMessage = errorMessage
    }

    public var body: some View {
        let shouldShowErrorMessage = !isValid && wasTouched

        VStack(spacing: boxSpacing.small) {
            TextField(label, text: $value)
                .border(shouldShowErrorMessage ? Color(sharedResource: .error) : Color.clear)
                .focused($wasTouched)

            if shouldShowErrorMessage, let errorMessage = errorMessage {
                Text(errorMessage)
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

#Preview("Valid") {
    ValidatableTextField(label: "Label", value: .constant("value"), isValid: true, errorMessage: nil)
}

#Preview("Invalid") {
    ValidatableTextField(label: "Label", value: .constant("value"), isValid: false, errorMessage: "Not matching pattern")
}
