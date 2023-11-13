import SwiftUI

public struct MessageView: View {
    let icon: Image
    let message: String
    
    public init(icon: Image, message: String) {
        self.icon = icon
        self.message = message
    }
    
    public var body: some View {
        VStack {
            icon
                .resizable()
                .frame(width: 40, height: 40)
            
            Text(message)
        }
    }
}

struct Message_Previews: PreviewProvider {
    static var previews: some View {
        MessageView(
            icon: Image(.sentimentDissatisfied),
            message: "Nothing to show")
    }
}
