import Foundation
import Session

struct LoginCredentialModel: Equatable {
    var username: String
    var password: String
}

extension LoginCredentialModel {
    func isValid() -> Bool {
        return Username.isValid(value: self.username)
        && Password.isValid(value: self.password)
    }
}
