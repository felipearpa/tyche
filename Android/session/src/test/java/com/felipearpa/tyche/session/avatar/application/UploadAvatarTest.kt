package com.felipearpa.tyche.session.avatar.application

import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.avatar.domain.AvatarRepository
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UploadAvatarTest {

    private val storedBundle = AccountBundle(
        accountId = "account-1",
        externalAccountId = "external-1",
        email = "gambler@tyche.com",
    )

    @Test
    fun `given a stored account when executed then a url is issued for it and the image is uploaded`() =
        runTest {
            val avatarRepository = mockk<AvatarRepository>()
            val accountStorage = mockk<AccountStorage>()
            val imageData = byteArrayOf(1, 2, 3)
            coEvery { accountStorage.retrieve() } returns storedBundle
            coEvery {
                avatarRepository.issueUploadUrl(accountId = "account-1", contentLength = 3L)
            } returns Result.success(ISSUED_URL)
            coEvery {
                avatarRepository.upload(url = ISSUED_URL, data = imageData)
            } returns Result.success(Unit)

            val result = UploadAvatar(avatarRepository, accountStorage).execute(imageData)

            result.shouldBeSuccess()
            coVerify(exactly = 1) {
                avatarRepository.issueUploadUrl(accountId = "account-1", contentLength = 3L)
            }
            coVerify(exactly = 1) { avatarRepository.upload(url = ISSUED_URL, data = imageData) }
        }

    @Test
    fun `given no stored account when executed then it fails and nothing is issued nor uploaded`() =
        runTest {
            val avatarRepository = mockk<AvatarRepository>()
            val accountStorage = mockk<AccountStorage>()
            coEvery { accountStorage.retrieve() } returns null

            val result = UploadAvatar(avatarRepository, accountStorage).execute(byteArrayOf(1))

            result.shouldBeFailure { exception ->
                exception.shouldBeInstanceOf<IllegalStateException>()
            }
            coVerify(exactly = 0) { avatarRepository.issueUploadUrl(any(), any()) }
            coVerify(exactly = 0) { avatarRepository.upload(any(), any()) }
        }

    @Test
    fun `given the presign call fails when executed then the error propagates and nothing is uploaded`() =
        runTest {
            val avatarRepository = mockk<AvatarRepository>()
            val accountStorage = mockk<AccountStorage>()
            val presignException = RuntimeException("presign failed")
            coEvery { accountStorage.retrieve() } returns storedBundle
            coEvery {
                avatarRepository.issueUploadUrl(any(), any())
            } returns Result.failure(presignException)

            val result = UploadAvatar(avatarRepository, accountStorage).execute(byteArrayOf(1))

            result.shouldBeFailure { exception -> exception shouldBe presignException }
            coVerify(exactly = 0) { avatarRepository.upload(any(), any()) }
        }

    @Test
    fun `given the upload PUT fails when executed then the error propagates`() = runTest {
        val avatarRepository = mockk<AvatarRepository>()
        val accountStorage = mockk<AccountStorage>()
        val uploadException = RuntimeException("upload failed")
        coEvery { accountStorage.retrieve() } returns storedBundle
        coEvery { avatarRepository.issueUploadUrl(any(), any()) } returns Result.success(ISSUED_URL)
        coEvery { avatarRepository.upload(any(), any()) } returns Result.failure(uploadException)

        val result = UploadAvatar(avatarRepository, accountStorage).execute(byteArrayOf(1))

        result.shouldBeFailure { exception -> exception shouldBe uploadException }
    }
}

private const val ISSUED_URL =
    "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg"
