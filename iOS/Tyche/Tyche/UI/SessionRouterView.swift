import SwiftUI
import Swinject
import UI
import Core
import Session
import Account
import DataPool
import Pool

struct SessionRouterView: View {
    @State private var signedInAccountBundle: AccountBundle?? = nil
    @State var universalLink: URL? = nil
    @Environment(\.diResolver) var diResolver: DIResolver

    var body: some View {
        ZStack {
            switch signedInAccountBundle {
            case nil:
                Color.clear
            case .some(nil):
                let signInLinkUrlTemplate = diResolver.resolve(SignInLinkUrlTemplateProvider.self)!

                if let url = universalLink,
                   let params = matchURL(url.absoluteString, to: String(format: signInLinkUrlTemplate(), "{email}")),
                   let email = params["email"] {

                    EmailLinkSignInRouter(email: email, emailLink: url.absoluteString)
                } else {
                    HomeRouter(onSignIn: { accountBundle in signedInAccountBundle = accountBundle })
                }
            case .some(.some(let accountBundle)):
                let joinPoolUrlTemplate = diResolver.resolve(JoinPoolUrlTemplateProvider.self)!

                if let url = universalLink,
                   let params = matchURL(url.absoluteString, to: String(format: joinPoolUrlTemplate(), "{poolId}")),
                   let poolId = params["poolId"] {
                    PoolJoinerView(
                        viewModel: PoolJoinerViewModel(
                            getPoolUseCase: diResolver.resolve(GetPoolUseCase.self)!,
                            joinPoolUseCase: diResolver.resolve(JoinPoolUseCase.self)!,
                        ),
                        poolId: poolId,
                        gamblerId: accountBundle.accountId,
                        onJoinPool: { universalLink = nil },
                        onAbort: { universalLink = nil },
                    )
                } else {
                    PoolContent(accountBundle: accountBundle, onSignOut: { signedInAccountBundle = .some(nil) })
                }
            }
        }
        .task {
            let accountStorage = diResolver.resolve(AccountStorage.self)!
            signedInAccountBundle = try? await accountStorage.retrieve()
        }
        .onOpenURL { url in
            universalLink = url
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
                accountBundle: accountBundle,
                onPoolSelect: { newSelectedPool in selectedPool = newSelectedPool },
                onSignOut: onSignOut,
                poolScoreViewModel: PoolScoreListViewModel(
                    getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                        poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                    ),
                    joinPoolUrlTemplate: diResolver.resolve(JoinPoolUrlTemplateProvider.self)!,
                    gamblerId: accountBundle.accountId
                )
            )
        } else {
            PoolHomeRouter(
                user: accountBundle,
                pool: selectedPool!,
            )
        }
    }
}
