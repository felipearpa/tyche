import SwiftUI

public struct PostionIndicator: View {
    let position: Int
    let shouldUsePrimeryColor: Bool

    public var body: some View {
        ZStack {
            Text(String(position))
        }
        .frame(width: indicatorSize, height: indicatorSize)
        .background(shouldUsePrimeryColor ? Color(sharedResource: .primaryContainer) : Color(sharedResource: .secondaryContainer))
        .foregroundColor(shouldUsePrimeryColor ? Color(sharedResource: .onPrimaryContainter) : Color(sharedResource: .onSecondaryContainer))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private let indicatorSize = 32.0

#Preview("signed in") {
    PostionIndicator(position: 1, shouldUsePrimeryColor: true)
}

#Preview("not signed in") {
    PostionIndicator(position: 1, shouldUsePrimeryColor: true)
}
