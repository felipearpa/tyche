import Foundation
import SwiftUI
import UIKit

public struct AccountAvatarFallback {
    let identity: String
    let colorKey: String
    let backgroundColor: Color?
    let foregroundColor: Color?

    public init(
        identity: String,
        colorKey: String? = nil,
        backgroundColor: Color? = nil,
        foregroundColor: Color? = nil
    ) {
        self.identity = identity
        self.colorKey = colorKey ?? identity
        self.backgroundColor = backgroundColor
        self.foregroundColor = foregroundColor
    }
}

/// The account's avatar photo with the letter avatar as fallback — the single component
/// behind every surface that shows a gambler. Requests the photo through the shared
/// `AvatarImageStore` at the size this view actually renders, so all surfaces reuse one
/// decoded image and one in-flight load, and every consumer updates together when an
/// upload or a changed revalidation replaces the photo.
public struct AccountAvatar: View {
    private let accountId: String
    private let fallback: AccountAvatarFallback

    @State private var photo: UIImage?
    @State private var photoAccountId: String = ""
    @State private var measuredPixelSize: CGFloat = 0
    @ObservedObject private var revisions = AvatarStoreRevisions.shared
    @Environment(\.displayScale) private var displayScale

    public init(accountId: String, email: String) {
        self.accountId = accountId
        self.fallback = AccountAvatarFallback(
            identity: email.emailAvatarIdentity,
            colorKey: email
        )
    }

    public init(
        accountId: String,
        fallback: AccountAvatarFallback
    ) {
        self.accountId = accountId
        self.fallback = fallback
    }

    public var body: some View {
        ZStack {
            if let photo {
                Image(uiImage: photo)
                    .resizable()
                    .scaledToFill()
            } else {
                InitialAvatar(
                    identity: fallback.identity,
                    colorKey: fallback.colorKey,
                    backgroundColor: fallback.backgroundColor,
                    foregroundColor: fallback.foregroundColor
                )
            }
        }
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear { measure(proxy.size) }
                    .onChange(of: proxy.size) { newSize in measure(newSize) }
            }
        )
        .task(id: loadIdentity) {
            // Only an account change discards the shown photo; revision and size changes
            // keep it until the replacement arrives, so an upload swap never flashes the
            // fallback while a reused row can never show the previous gambler's photo.
            if photoAccountId != accountId {
                photo = nil
                photoAccountId = accountId
            }
            guard !accountId.isEmpty, measuredPixelSize > 0 else {
                photo = nil
                return
            }
            photo = await AvatarImageStore.shared.image(
                accountId: accountId,
                targetPixelSize: measuredPixelSize
            )
            // A cached absence keeps this surface on the fallback; retry when it lapses so
            // a photo uploaded on another device appears without leaving the screen.
            while photo == nil, !Task.isCancelled {
                guard
                    let delay = await AvatarImageStore.shared.missingRetryDelay(accountId: accountId)
                else { return }
                try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                guard !Task.isCancelled else { return }
                photo = await AvatarImageStore.shared.image(
                    accountId: accountId,
                    targetPixelSize: measuredPixelSize
                )
            }
        }
    }

    private func measure(_ size: CGSize) {
        measuredPixelSize = max(size.width, size.height) * displayScale
    }

    private var loadIdentity: String {
        // Unmeasured must be a distinct identity: a view whose real bucket matches the
        // unmeasured default would otherwise never restart the load after measuring.
        let bucket = measuredPixelSize > 0 ? avatarPixelBucket(forTargetPixelSize: measuredPixelSize) : -1
        return "\(accountId)#\(revisions.revision(for: accountId))#\(bucket)"
    }
}

func avatarPhotoRequest(accountId: String) -> URLRequest? {
    guard let url = AvatarURL.of(accountId: accountId) else { return nil }

    var request = URLRequest(url: url)
    request.cachePolicy = .reloadRevalidatingCacheData
    return request
}
