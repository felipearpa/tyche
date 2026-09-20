import Foundation
import Core

class AccountStorageInUserDefaults: AccountStorage {
    let storageInUserDefaults: StorageInUserDefaults

    init(storageInUserDefaults: StorageInUserDefaults) {
        self.storageInUserDefaults = storageInUserDefaults
    }

    func store(snapshot: CurrentAccountSnapshot) async throws {
        let raw = try JSONEncoder().encode(snapshot)
        try await storageInUserDefaults.store(raw: raw)
    }

    func delete() async throws {
        try await storageInUserDefaults.delete()
    }

    func retrieve() async throws -> CurrentAccountSnapshot? {
        guard let raw = try await storageInUserDefaults.retrieve() else { return nil }
        return try CurrentAccountSnapshot.decode(from: raw)
    }
}
