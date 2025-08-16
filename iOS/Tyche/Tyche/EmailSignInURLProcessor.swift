import Foundation

struct EmailSignInURLProcessor {
    let link: URL
    
    init(link: URL) {
        self.link = link
    }
    
    func email() -> String? {
        let pathComponents = self.link.pathComponents
        if let extractedEmail = pathComponents.last {
            return extractedEmail
        }
        return nil
    }
    
    func emailLink() -> String {
        link.absoluteString
    }
}
