import SwiftUI
import Swinject
import FirebaseCore
import Core
import Session
import DataPool
import DataBet

@main
struct TycheApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var diResolver = DIResolver(
        resolver:Assembler([
            CoreAssembly(),
            LoginAssembly(),
            PoolAssembly(),
            BetAssembly(),
            TycheAssembly(),
        ]).resolver)
    
    var body: some Scene {
        WindowGroup {
            RootRouterView()
                .environment(\.diResolver, diResolver)
        }
    }
}
