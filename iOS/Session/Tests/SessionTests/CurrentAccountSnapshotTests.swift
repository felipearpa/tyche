import Foundation
import Testing
@testable import Session

@Suite("CurrentAccountSnapshot")
struct CurrentAccountSnapshotTests {

    @Test("given a legacy raw bundle payload when decoded then it migrates as a stale snapshot")
    func legacyBundleMigratesAsStale() throws {
        let legacy = Data(
            """
            {"accountId":"account-1","externalAccountId":"external-1","email":"gambler@tyche.com","username":"ElGoleador"}
            """.utf8
        )

        let snapshot = try CurrentAccountSnapshot.decode(from: legacy)

        #expect(snapshot.account.accountId == "account-1")
        #expect(snapshot.account.externalAccountId == "external-1")
        #expect(snapshot.account.email == "gambler@tyche.com")
        #expect(snapshot.account.username == "ElGoleador")
        #expect(snapshot.validatedAt == nil)
    }

    @Test("given a legacy payload without optional fields when decoded then no field is lost or invented")
    func minimalLegacyBundleMigrates() throws {
        let legacy = Data(
            """
            {"accountId":"account-1","externalAccountId":"external-1"}
            """.utf8
        )

        let snapshot = try CurrentAccountSnapshot.decode(from: legacy)

        #expect(snapshot.account.accountId == "account-1")
        #expect(snapshot.account.email == "")
        #expect(snapshot.validatedAt == nil)
    }

    @Test("given an envelope payload when decoded then the validation timestamp survives the round trip")
    func envelopeRoundTripsWithValidation() throws {
        let original = CurrentAccountSnapshot(
            account: AccountBundle(
                accountId: "account-1",
                externalAccountId: "external-1",
                email: "gambler@tyche.com",
                username: "ElGoleador"
            ),
            validatedAt: Date(timeIntervalSince1970: 12345)
        )
        let raw = try JSONEncoder().encode(original)

        let decoded = try CurrentAccountSnapshot.decode(from: raw)

        #expect(decoded == original)
    }

    @Test("given an envelope payload without a validation timestamp then it decodes as stale")
    func envelopeWithoutValidationDecodesAsStale() throws {
        let original = CurrentAccountSnapshot(
            account: AccountBundle(accountId: "account-1", externalAccountId: "external-1", email: "a@b.c"),
            validatedAt: nil
        )
        let raw = try JSONEncoder().encode(original)

        let decoded = try CurrentAccountSnapshot.decode(from: raw)

        #expect(decoded == original)
    }

    @Test("given an unrecognizable payload when decoded then it throws instead of fabricating an account")
    func garbageThrows() {
        let garbage = Data("{\"something\":true}".utf8)

        #expect(throws: (any Error).self) {
            try CurrentAccountSnapshot.decode(from: garbage)
        }
    }
}
