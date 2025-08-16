import Foundation
import Core

class AuthStorageInUserDefaults: AuthStorage {
    let storageInUserDefaults: StorageInUserDefaults
    
    init(storageInUserDefaults: StorageInUserDefaults) {
        self.storageInUserDefaults = storageInUserDefaults
    }
    
    func store(authBundle: AuthBundle) async throws {
        let raw = try JSONEncoder().encode(authBundle)
        try await storageInUserDefaults.store(raw: raw)
    }
    
    func delete() async throws {
        try await storageInUserDefaults.delete()
    }
    
    func retrieve() async throws -> AuthBundle? {
        guard let raw = try await storageInUserDefaults.retrieve() else { return nil }
        return try JSONDecoder().decode(AuthBundle.self, from: raw)
    }
}
