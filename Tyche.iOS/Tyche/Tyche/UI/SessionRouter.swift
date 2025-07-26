import SwiftUI
import Swinject
import UI
import Core
import Session
import Account
import DataPool
import Pool

struct SessionRouter: View {
    @State private var signedInAccountBundle: AccountBundle?? = nil
    @Environment(\.diResolver) var diResolver: DIResolver

    var body: some View {
        Group {
            switch signedInAccountBundle {
            case nil:
                EmptyView()
            case .some(nil):
                HomeRouter(onSignIn: { accountBundle in signedInAccountBundle = accountBundle })
            case .some(.some(let bundle)):
                PoolContent(accountBundle: bundle, onSignOut: { signedInAccountBundle = .some(nil) })
            }
        }
        .task {
            let accountStorage = diResolver.resolve(AccountStorage.self)!
            if let accountBundle = try? await accountStorage.retrieve() {
                signedInAccountBundle = accountBundle
            }
        }
    }
}

struct PoolContent: View {
    let accountBundle: AccountBundle
    let onSignOut: () -> Void

    @State private var selectedPool: PoolProfile? = nil
    @Environment(\.diResolver) private var diResolver: DIResolver

    var body: some View {
        if selectedPool == nil {
            PoolScoreListRouter(
                user: accountBundle,
                onPoolSelected: { newSelectedPool in selectedPool = newSelectedPool },
                onLogout: onSignOut,
            )
        } else {
            PoolHomeRouter(
                user: accountBundle,
                pool: selectedPool!,
            )
        }
    }
}
