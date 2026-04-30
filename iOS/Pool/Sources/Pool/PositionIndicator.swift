import SwiftUI

public struct PostionIndicator: View {
    let postion: Int
    let isSignedIdUser: Bool

    public var body: some View {
        ZStack {
            Text(String(postion))
        }
        .frame(width: indicatorSize, height: indicatorSize)
        .background(isSignedIdUser ? Color(sharedResource: .primaryContainer) : Color(sharedResource: .secondaryContainer))
        .foregroundColor(isSignedIdUser ? Color(sharedResource: .onPrimaryContainter) : Color(sharedResource: .onSecondaryContainer))
        .clipShape(Circle())
    }
}

private let indicatorSize = 32.0

#Preview("signed in") {
    PostionIndicator(postion: 1, isSignedIdUser: true)
}

#Preview("not signed in") {
    PostionIndicator(postion: 1, isSignedIdUser: true)
}
