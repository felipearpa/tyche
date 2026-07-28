import Foundation
import SwiftUI
import UIKit

/// Bumped after the signed-in account's avatar changes, so navigation chrome
/// showing the photo refetches instead of waiting for a fresh appearance.
public final class AvatarVersion: ObservableObject {
    public static let shared = AvatarVersion()

    @Published public private(set) var value = 0

    public func bump() {
        value += 1
    }
}

/// The account's avatar photo with the letter avatar as fallback — the single component
/// behind every surface that shows the signed-in gambler. Loads with a revalidating cache
/// policy so a photo replaced elsewhere is picked up instead of a superseded cached copy.
public struct AccountAvatar: View {
    private let accountId: String
    private let email: String

    @State private var photo: UIImage?
    @ObservedObject private var avatarVersion = AvatarVersion.shared

    public init(accountId: String, email: String) {
        self.accountId = accountId
        self.email = email
    }

    public var body: some View {
        ZStack {
            if let photo {
                Image(uiImage: photo)
                    .resizable()
                    .scaledToFill()
            } else {
                EmailAvatar(email: email)
            }
        }
        .task(id: "\(accountId)#\(avatarVersion.value)") {
            photo = await loadPhoto()
        }
    }

    private func loadPhoto() async -> UIImage? {
        guard let url = AvatarURL.of(accountId: accountId) else { return nil }

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadRevalidatingCacheData

        guard
            let (data, response) = try? await URLSession.shared.data(for: request),
            (response as? HTTPURLResponse)?.statusCode == 200
        else { return nil }

        return UIImage(data: data)
    }
}
