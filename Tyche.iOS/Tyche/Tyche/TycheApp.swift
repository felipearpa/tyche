import SwiftUI
import Swinject
import Core
import Session
import DataPool
import DataBet

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
