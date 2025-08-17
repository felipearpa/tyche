import Foundation
import Core

class LocalAndroidPackageNameProvider: AndroidPackageNameProvider {
    func callAsFunction() -> String {
        return Bundle.main.object(forInfoDictionaryKey: "ANDROID_PACKAGE_NAME") as? String ?? ""
    }
}
