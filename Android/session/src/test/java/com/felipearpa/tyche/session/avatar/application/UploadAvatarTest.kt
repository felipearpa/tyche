package com.felipearpa.tyche.session.avatar.application

import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.avatar.domain.AvatarRepository
import com.felipearpa.tyche.session.avatar.domain.InstallUploadedAvatar
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UploadAvatarTest {

    private val storedBundle = AccountBundle(
        accountId = "account-1",
        externalAccountId = "external-1",
        email = "gambler@tyche.com",
    )

    private fun coordinatorPublishing(bundle: AccountBundle?): CurrentAccountCoordinator =
        mockk<CurrentAccountCoordinator> {
            every { state } returns MutableStateFlow(bundle)
        }

    private fun seeder(): InstallUploadedAvatar {
        val installUploadedAvatar = mockk<InstallUploadedAvatar>()
        coJustRun { installUploadedAvatar.invoke(any(), any()) }
        return installUploadedAvatar
    }

    @Test
    fun `given a published account when executed then a url is issued the image is uploaded and the store is seeded before returning`() =
        runTest {
            val avatarRepository = mockk<AvatarRepository>()
            val coordinator = coordinatorPublishing(storedBundle)
            val installUploadedAvatar = seeder()
            val imageData = byteArrayOf(1, 2, 3)
            coEvery {
                avatarRepository.issueUploadUrl(accountId = "account-1", contentLength = 3L)
            } returns Result.success(ISSUED_URL)
            coEvery {
                avatarRepository.upload(url = ISSUED_URL, data = imageData)
            } returns Result.success(Unit)

            val result = UploadAvatar(avatarRepository, coordinator, installUploadedAvatar)
                .execute(imageData)

            result.shouldBeSuccess()
            coVerify(exactly = 1) {
                avatarRepository.issueUploadUrl(accountId = "account-1", contentLength = 3L)
            }
            coVerify(exactly = 1) { avatarRepository.upload(url = ISSUED_URL, data = imageData) }
            coVerify(exactly = 1) { installUploadedAvatar.invoke(imageData, "account-1") }
            coVerifyOrder {
                avatarRepository.upload(url = ISSUED_URL, data = imageData)
                installUploadedAvatar.invoke(imageData, "account-1")
            }
        }

    @Test
    fun `given no published account when executed then it fails and nothing is issued uploaded nor seeded`() =
        runTest {
            val avatarRepository = mockk<AvatarRepository>()
            val coordinator = coordinatorPublishing(null)
            val installUploadedAvatar = seeder()

            val result = UploadAvatar(avatarRepository, coordinator, installUploadedAvatar)
                .execute(byteArrayOf(1))

            result.shouldBeFailure { exception ->
                exception.shouldBeInstanceOf<IllegalStateException>()
            }
            coVerify(exactly = 0) { avatarRepository.issueUploadUrl(any(), any()) }
            coVerify(exactly = 0) { avatarRepository.upload(any(), any()) }
            coVerify(exactly = 0) { installUploadedAvatar.invoke(any(), any()) }
        }

    @Test
    fun `given the presign call fails when executed then the error propagates and nothing is uploaded nor seeded`() =
        runTest {
            val avatarRepository = mockk<AvatarRepository>()
            val coordinator = coordinatorPublishing(storedBundle)
            val installUploadedAvatar = seeder()
            val presignException = RuntimeException("presign failed")
            coEvery {
                avatarRepository.issueUploadUrl(any(), any())
            } returns Result.failure(presignException)

            val result = UploadAvatar(avatarRepository, coordinator, installUploadedAvatar)
                .execute(byteArrayOf(1))

            result.shouldBeFailure { exception -> exception shouldBe presignException }
            coVerify(exactly = 0) { avatarRepository.upload(any(), any()) }
            coVerify(exactly = 0) { installUploadedAvatar.invoke(any(), any()) }
        }

    @Test
    fun `given the upload PUT fails when executed then the error propagates and nothing is seeded`() =
        runTest {
            val avatarRepository = mockk<AvatarRepository>()
            val coordinator = coordinatorPublishing(storedBundle)
            val installUploadedAvatar = seeder()
            val uploadException = RuntimeException("upload failed")
            coEvery {
                avatarRepository.issueUploadUrl(any(), any())
            } returns Result.success(ISSUED_URL)
            coEvery {
                avatarRepository.upload(any(), any())
            } returns Result.failure(uploadException)

            val result = UploadAvatar(avatarRepository, coordinator, installUploadedAvatar)
                .execute(byteArrayOf(1))

            result.shouldBeFailure { exception -> exception shouldBe uploadException }
            coVerify(exactly = 0) { installUploadedAvatar.invoke(any(), any()) }
        }
}

private const val ISSUED_URL =
    "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg"
