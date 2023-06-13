import Foundation

struct LoginCredentialModel : Equatable {
    var username: String
    var password: String
}

extension LoginCredentialModel {
    
    func isValid() -> Bool {
        return Username.isValid(value: self.username)
        && Password.isValid(value: self.password)
    }
    
    func toInput() -> LoginInput {
        return LoginInput(
            username: Username(value: self.username)!,
            password: Password(value: self.password)!
        )
    }
}
