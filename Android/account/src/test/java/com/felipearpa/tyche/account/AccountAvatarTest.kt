package com.felipearpa.tyche.account

import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AccountAvatarTest {
    @Test
    fun `email compatibility wrapper keeps using the local part for its initial`() {
        "felipe.arpa".avatarInitial() shouldBe 'F'
        "".avatarInitial() shouldBe null
    }

    @Test
    fun `username fallback uses the first available letter or number`() {
        "__élgoleador".avatarInitial() shouldBe 'É'
        "⚽ 88".avatarInitial() shouldBe '8'
        "___".avatarInitial() shouldBe null
    }

    @Test
    fun `generated avatar colors meet enhanced contrast in both themes`() {
        listOf(false, true).forEach { isDark ->
            val (background, foreground) = avatarColorsFor(
                key = "ElGoleador",
                isDark = isDark,
            )
            contrastRatio(background, foreground).shouldBeGreaterThanOrEqual(7.0)
        }
    }

    @Test
    fun `avatar request uses the deterministic public URL and revalidates`() {
        val request = avatarPhotoRequestConfiguration(
            accountId = "account-1",
            version = 3,
        )

        request.url shouldBe
            "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg"
        request.cacheControl shouldBe "no-cache"
        request.memoryCacheKey shouldBe "avatar#account-1#3"
        request.diskCacheKey shouldBe request.url
    }
}
