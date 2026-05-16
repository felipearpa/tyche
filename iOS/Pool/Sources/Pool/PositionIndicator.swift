import SwiftUI

public struct PostionIndicator: View {
    let postion: Int
    let shouldUsePrimeryColor: Bool

    public var body: some View {
        ZStack {
            Text(String(postion))
        }
        .frame(width: indicatorSize, height: indicatorSize)
        .background(shouldUsePrimeryColor ? Color(sharedResource: .primaryContainer) : Color(sharedResource: .secondaryContainer))
        .foregroundColor(shouldUsePrimeryColor ? Color(sharedResource: .onPrimaryContainter) : Color(sharedResource: .onSecondaryContainer))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private let indicatorSize = 32.0

#Preview("signed in") {
    PostionIndicator(postion: 1, shouldUsePrimeryColor: true)
}

#Preview("not signed in") {
    PostionIndicator(postion: 1, shouldUsePrimeryColor: true)
}
