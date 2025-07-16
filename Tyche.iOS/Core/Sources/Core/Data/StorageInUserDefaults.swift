import CryptoKit
import Foundation

public class StorageInUserDefaults {
    private let key: String
    private let suite: String
    private let userDefaults: UserDefaults

    public init(key: String, suite: String) {
        self.key = key
        self.suite = suite
        userDefaults = UserDefaults(suiteName: suite)!
    }

    public func store(raw: Data) async throws {
        let encryptedSealedBox = try AES.GCM.seal(raw, using: symmetricKey())
        userDefaults.set(encryptedSealedBox.combined, forKey: encriptedKey())
    }

    public func delete() async throws {
        userDefaults.removeObject(forKey: encriptedKey())
    }

    public func retrieve() async throws -> Data? {
        guard let encryptedData = userDefaults.data(forKey: encriptedKey()) else {
            return nil
        }

        let encryptedSealedBox = try AES.GCM.SealedBox(combined: encryptedData)
        guard let decryptedData = try? AES.GCM.open(encryptedSealedBox, using: symmetricKey()) else {
            return nil
        }

        return decryptedData
    }

    private func encriptedKey() -> String {
        return SHA256.hash(data: Data(key.utf8)).compactMap { value in String(format: "%02x", value) }.joined()
    }

    private func symmetricKey() -> SymmetricKey {
        let tag = "\(Bundle.main.bundleIdentifier!).keys.\(key)"
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
