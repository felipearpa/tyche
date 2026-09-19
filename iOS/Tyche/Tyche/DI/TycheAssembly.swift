import Account
import Swinject
import Session
import Core

public class TycheAssembly : Assembly {
    public init() {}

    public func assemble(container: Swinject.Container) {
        container.register(URLBasePathProvider.self) { _ in
            LocalURLBasePathProvider()
        }

        // The upload application flow seeds the shared avatar store before reporting
        // success (design: centralize-account-and-avatar-caching, decision 6); the app
        // supplies the store binding because Session cannot depend on Account.
        container.register(InstallUploadedAvatar.self) { _ in
            { imageData, accountId in
                await AvatarImageStore.shared.installUploadedAvatar(
                    jpegData: imageData,
                    accountId: accountId
                )
            }
        }

        container.register(SignInLinkUrlTemplateProvider.self) { _ in
            LocalSignInLinkUrlTemplateProvider()
        }

        container.register(AndroidPackageNameProvider.self) { _ in
            LocalAndroidPackageNameProvider()
        }

        container.register(JoinPoolUrlTemplateProvider.self) { _ in
            LocalJoinPoolUrlTemplateProvider()
        }
    }
}
