import SwiftUI

public struct PlainToolbarItem<Content: View>: ToolbarContent {
    let placement: ToolbarItemPlacement
    let content: Content

    public init(
        placement: ToolbarItemPlacement = .automatic,
        @ViewBuilder content: () -> Content,
    ) {
        self.placement = placement
        self.content = content()
    }

    public var body: some ToolbarContent {
        if #available(iOS 26.0, *) {
            ToolbarItem(placement: placement) {
                content
            }
            .sharedBackgroundVisibility(.hidden)
        } else {
            ToolbarItem(placement: placement) {
                content
            }
        }
    }
}
