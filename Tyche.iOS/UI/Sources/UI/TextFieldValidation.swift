public struct TextFieldValidation {
    public let isValid: (String) -> Bool
    public let errorMessage: String?

    public init(
        isValid: @escaping (String) -> Bool,
        errorMessage: String? = nil,
    ) {
        self.isValid = isValid
        self.errorMessage = errorMessage
    }
}
