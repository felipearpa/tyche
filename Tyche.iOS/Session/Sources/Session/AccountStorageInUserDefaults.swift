import Foundation
import Core

class AccountStorageInUserDefaults: AccountStorage {
    let storageInUserDefaults: StorageInUserDefaults
    
    init(storageInUserDefaults: StorageInUserDefaults) {
        self.storageInUserDefaults = storageInUserDefaults
    }
    
    func store(accountBundle: AccountBundle) async throws {
        let raw = try JSONEncoder().encode(accountBundle)
        try await storageInUserDefaults.store(raw: raw)
    }
    
    func delete() async throws {
        try await storageInUserDefaults.delete()
    }
    
    func retrieve() async throws -> AccountBundle? {
        guard let raw = try await storageInUserDefaults.retrieve() else { return nil }
        return try JSONDecoder().decode(AccountBundle.self, from: raw)
    }
}
