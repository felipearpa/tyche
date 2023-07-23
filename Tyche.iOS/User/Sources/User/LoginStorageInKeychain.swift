import CryptoKit
import Foundation

private let KEY = "loginProfile"
private let SUITE = "login"

class LoginStorageInKeychain : LoginStorage {
    let loginDefaults = UserDefaults(suiteName: SUITE)!
    
    func store(loginProfile: LoginProfile) throws {
        let loginProfileJsonData = try JSONEncoder().encode(loginProfile)
        let encryptedSealedBox = try AES.GCM.seal(loginProfileJsonData, using: encriptionKey())
        loginDefaults.set(encryptedSealedBox.combined, forKey: key())
    }
    
    func get() throws -> LoginProfile? {
        guard let encryptedUserProfileData = loginDefaults.data(forKey: key()) else {
            return nil
        }
        
        let encryptedSealedBox = try AES.GCM.SealedBox(combined: encryptedUserProfileData)
        
        guard let decryptedData = try? AES.GCM.open(encryptedSealedBox, using: encriptionKey()) else {
            return nil
        }
        
        return try JSONDecoder().decode(LoginProfile.self, from: decryptedData)
    }
    
    private func key() -> String {
        return SHA256.hash(data: Data(KEY.utf8)).compactMap { value in String(format: "%02x", value) }.joined()
    }
    
    private func encriptionKey() -> SymmetricKey {
        let tag = "\(Bundle.main.bundleIdentifier!).keys.\(KEY)"
        
        guard let encriptionKey = retrieveEncryptionKeyFromKeychain(tag: tag) else {
            return generateEncryptionKeyInKeychain(tag: tag)!
        }
        
        return encriptionKey
    }
    
    private func retrieveEncryptionKeyFromKeychain(tag: String) -> SymmetricKey? {
        let query: [String: Any] = [kSecClass as String: kSecClassKey,
                                    kSecAttrApplicationTag as String: tag,
                                    kSecReturnData as String: kCFBooleanTrue!,
                                    kSecMatchLimit as String: kSecMatchLimitOne]
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess else {
            return nil
        }

        let encryptionKeyData = item as! Data
        return SymmetricKey(data: encryptionKeyData)
    }
    
    private func generateEncryptionKeyInKeychain(tag: String) -> SymmetricKey? {
        let encriptionKey = SymmetricKey(size: .bits256)
        let keyData = encriptionKey.withUnsafeBytes { bytes in Data(bytes) }
        let query: [String: Any] = [kSecClass as String: kSecClassKey,
                                    kSecAttrApplicationTag as String: tag,
                                    kSecValueData as String: keyData]
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            return nil
        }

        return encriptionKey
    }
}
