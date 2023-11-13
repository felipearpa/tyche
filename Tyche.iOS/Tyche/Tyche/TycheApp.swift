import SwiftUI
import Swinject
import Core
import User
import Pool
import Bet

@main
struct TycheApp: App {
    var diResolver = DIResolver(
        resolver:Assembler([
            CoreAssembly(),
            LoginAssembly(),
            PoolAssembly(),
            BetAssembly()
        ]).resolver)
    
    var body: some Scene {
        WindowGroup {
            MainView(diResolver: diResolver)
                .environmentObject(diResolver)
        }
    }
}
