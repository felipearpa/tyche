package com.felipearpa.tyche.profile

import com.felipearpa.tyche.account.AvatarUrl
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AvatarUrlTest {

    @Test
    fun `given an account id when derived then the url is a pure function of the id`() {
        AvatarUrl.of("01KQ5M0GTPJQTCMPC13993A8VQ") shouldBe
            "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/01KQ5M0GTPJQTCMPC13993A8VQ.jpg"
    }
}
