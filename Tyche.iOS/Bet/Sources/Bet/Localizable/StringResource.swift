import Foundation

struct StringResource {
    fileprivate let name: String.LocalizationValue

    fileprivate let bundle: Bundle

    init(name: String.LocalizationValue, bundle: Bundle) {
        self.name = name
        self.bundle = bundle
    }
}

extension StringResource {
    static let forbiddenBetFailureDescription = StringResource(name: "forbidden_bet_failure_description", bundle: Bundle.module)
    static let forbiddenBetFailureReason = StringResource(name: "forbidden_bet_failure_reason", bundle: Bundle.module)
}

extension String {
    init(_ resource: StringResource) {
        self.init(localized: resource.name, bundle: resource.bundle)
    }
}
