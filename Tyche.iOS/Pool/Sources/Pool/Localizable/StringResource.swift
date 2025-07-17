import SwiftUI

struct StringResource {
    fileprivate let name: String.LocalizationValue

    fileprivate let bundle: Bundle

    init(name: String.LocalizationValue, bundle: Bundle) {
        self.name = name
        self.bundle = bundle
    }
}

extension StringResource {
    static let positionLabel = StringResource(name: "position_label", bundle: Bundle.module)
    static let gamblerPoolListTitle = StringResource(name: "gambler_pool_list_title", bundle: Bundle.module)
    static let startingFromDateText = StringResource(name: "starting_from_date_text", bundle: Bundle.module)
    static let choosePoolLayoutText = StringResource(name: "choose_pool_layout_text", bundle: Bundle.module)
    static let fillPoolFieldsTitle = StringResource(name: "fill_pool_fields_title", bundle: Bundle.module)
    static let poolNameLabel = StringResource(name: "pool_name_label", bundle: Bundle.module)
    static let poolNameRequiredFailureText = StringResource(name: "pool_name_required_failure_text", bundle: Bundle.module)
    static let poolFromLayoutCreatorTitle = StringResource(name: "pool_from_layout_creator_title", bundle: Bundle.module)
}

extension String {
    init(_ resource: StringResource) {
        self.init(localized: resource.name, bundle: resource.bundle)
    }
}
