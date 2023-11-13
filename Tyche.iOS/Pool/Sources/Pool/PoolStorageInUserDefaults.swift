import CryptoKit
import Foundation

private let KEY = "poolProfile"

class PoolStorageInUserDefaults : PoolStorage {
    let poolDefaults = UserDefaults.standard
    
    func store(poolProfile: PoolProfile) async throws {
        let poolProfileJsonData = try JSONEncoder().encode(poolProfile)
        poolDefaults.set(poolProfileJsonData, forKey: KEY)
    }
    
    func get() async throws -> PoolProfile? {
        guard let poolProfileData = poolDefaults.data(forKey: KEY) else {
            return nil
        }
        
        return try JSONDecoder().decode(PoolProfile.self, from: poolProfileData)
    }
}
