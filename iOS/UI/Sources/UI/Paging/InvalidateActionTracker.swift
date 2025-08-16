import Foundation

typealias InvalidateAction = () -> Void

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
    
    func invalidate() {
        actions.forEach { pair in
            pair.value()
        }
    }
}
