import SwiftUI
import Swinject
import Core

struct MainView: View {
    let diResolver: DIResolver
    
    var body: some View {
        HomeRouter(diResolver: diResolver)
    }
}

#Preview {
    MainView(diResolver: DIResolver(resolver:Assembler([]).resolver))
}
