import SwiftUI
import Swinject
import FirebaseCore
import Core
import Session
import DataPool
import DataBet

@main
struct TycheApp: App {
    init() {
        FirebaseApp.configure()
    }

    var diResolver = DIResolver(
        resolver:Assembler([
            CoreAssembly(),
            LoginAssembly(),
            PoolAssembly(),
            BetAssembly()
        ]).resolver)
    
    var body: some Scene {
        WindowGroup {
            MainView()
                .environment(\.diResolver, diResolver)
        }
    }
}
