import Account
import Testing

@Suite("AvatarURL")
struct AvatarURLTests {

    @Test("given an account id when derived then the url is a pure function of the id")
    func derivesTheDeterministicUrl() {
        let url = AvatarURL.of(accountId: "01KQ5M0GTPJQTCMPC13993A8VQ")

        #expect(
            url?.absoluteString
                == "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/01KQ5M0GTPJQTCMPC13993A8VQ.jpg"
        )
    }

    @Test("given no account id when derived then there is no url and the letter avatar wins")
    func emptyAccountIdYieldsNoUrl() {
        #expect(AvatarURL.of(accountId: "") == nil)
    }
}
