import Foundation

struct JoinPoolURLProcessor {
    let link: URL

    init(link: URL) {
        self.link = link
    }

    func poolId() -> String? {
        let pathComponents = self.link.pathComponents
        if pathComponents.count >= 4,
           pathComponents[1] == "tyche",
           pathComponents[2] == "pools",
           pathComponents[4] == "join" {
            return pathComponents[3]
        }
        return nil
    }
}
