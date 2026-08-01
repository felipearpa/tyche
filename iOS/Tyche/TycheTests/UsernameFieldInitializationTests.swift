import Foundation
import Testing
@testable import Tyche

@Suite("UsernameFieldInitialization")
struct UsernameFieldInitializationTests {
    @Test("given a stored username when initialized then the field is populated with a collapsed caret after the final character")
    func prefilledInitialization() {
        var initialization = UsernameFieldInitialization()

        let result = initialization.takeInitialization(initialUsername: "felipearpa")

        #expect(result?.draft == "felipearpa")
        #expect(result?.caretIndex == 10)
    }

    @Test("given an empty stored username when initialized then the caret is placed at position 0")
    func emptyInitialization() {
        var initialization = UsernameFieldInitialization()

        let result = initialization.takeInitialization(initialUsername: "")

        #expect(result?.draft == "")
        #expect(result?.caretIndex == 0)
    }

    @Test("given multi-scalar graphemes then the caret index counts grapheme clusters")
    func graphemeCaretIndex() {
        var initialization = UsernameFieldInitialization()

        // Family emoji is one grapheme cluster made of several unicode scalars.
        let result = initialization.takeInitialization(initialUsername: "ab👨‍👩‍👧‍👦")

        #expect(result?.caretIndex == 3)
    }

    @Test("given a completed initialization then later appearances do not initialize again")
    func oneShotPerPresentation() {
        var initialization = UsernameFieldInitialization()

        #expect(initialization.takeInitialization(initialUsername: "felipearpa") != nil)
        #expect(initialization.takeInitialization(initialUsername: "felipearpa") == nil)
        #expect(initialization.takeInitialization(initialUsername: "other") == nil)
    }
}
