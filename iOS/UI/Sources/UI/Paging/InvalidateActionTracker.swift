import Foundation

typealias InvalidateAction = () async -> Void

class InvalidateActionTracker {
    private var actions: [String: InvalidateAction] = [:]
    
    func registerAction(_ action: @escaping InvalidateAction) -> String {
        let actionId = UUID().uuidString
        actions[actionId] = action
        return actionId
    }
    
    func unregisterAction(actionId: String) {
        actions.removeValue(forKey: actionId)
    }
    
    func invalidate() async {
        for action in actions.values {
            await action()
        }
    }
}
