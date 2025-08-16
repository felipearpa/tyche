import XCTest
@testable import Core

final class LocalURLBasePathProviderTests: XCTestCase {
    
    func testGivenABasePathWhenIsPreppendThenANewUrlWithTheBasePathPreppendedIsReturned() throws {
        let basePath = "https://www.basepath.com/"
        let suffixPath = "suffix"
        
        let urlBasePathProvider = LocalURLBasePathProvider(basePath: basePath)
        let maybeNewUrl = urlBasePathProvider.prependBasePath(suffixPath)
        
        guard let newUrl = maybeNewUrl else {
            XCTFail("Expected not null URL, but nul received")
            return
        }
        
        XCTAssertTrue(newUrl.absoluteString.hasPrefix(basePath))
        XCTAssertTrue(newUrl.absoluteString.hasSuffix(suffixPath))
    }
}
