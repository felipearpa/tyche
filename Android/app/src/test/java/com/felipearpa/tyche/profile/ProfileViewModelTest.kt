package com.felipearpa.tyche.profile

import android.graphics.Bitmap
import com.felipearpa.tyche.account.AvatarVersion
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.avatar.application.UploadAvatar
import com.felipearpa.ui.state.LoadState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given no stored account when created then the letter avatar is shown`() = runTest {
        val viewModel = ProfileViewModel(
            uploadAvatar = mockk(),
            accountStorage = FakeAccountStorage(bundle = null),
        )

        viewModel.avatarSource.value shouldBe ProfileAvatarSource.Letter
    }

    @Test
    fun `given a stored account when created then the avatar url derives from the account id`() =
        runTest {
            val viewModel = ProfileViewModel(
                uploadAvatar = mockk(),
                accountStorage = FakeAccountStorage(bundle = storedBundle),
            )

            viewModel.avatarSource.value shouldBe
                ProfileAvatarSource.Remote(
                    "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg",
                )
            viewModel.username.value shouldBe "ElGoleador"
            viewModel.email.value shouldBe "gambler@tyche.com"
        }

    @Test
    fun `given an upload begins then the new photo shows immediately and stays after success without a re-fetch`() =
        runTest {
            val uploadAvatar = mockk<UploadAvatar>()
            val gate = CompletableDeferred<Unit>()
            coEvery { uploadAvatar.execute(any()) } coAnswers {
                gate.await()
                Result.success(Unit)
            }
            val viewModel = ProfileViewModel(
                uploadAvatar = uploadAvatar,
                accountStorage = FakeAccountStorage(bundle = storedBundle),
            )
            val newPhoto = mockk<Bitmap>()
            val imageData = byteArrayOf(9, 9)

            val job = viewModel.beginUpload(bitmap = newPhoto, imageData = imageData)

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Loading>()
            viewModel.avatarSource.value shouldBe ProfileAvatarSource.Local(newPhoto)

            gate.complete(Unit)
            job.join()

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Loaded<Unit>>()
            viewModel.avatarSource.value shouldBe ProfileAvatarSource.Local(newPhoto)
            coVerify(exactly = 1) { uploadAvatar.execute(imageData) }
        }

    @Test
    fun `given the upload fails then the previous avatar is preserved and retry can succeed`() =
        runTest {
            val uploadAvatar = mockk<UploadAvatar>()
            coEvery { uploadAvatar.execute(any()) } returnsMany listOf(
                Result.failure(RuntimeException("upload failed")),
                Result.success(Unit),
            )
            val viewModel = ProfileViewModel(
                uploadAvatar = uploadAvatar,
                accountStorage = FakeAccountStorage(bundle = storedBundle),
            )
            val previousSource = viewModel.avatarSource.value
            val newPhoto = mockk<Bitmap>()

            viewModel.beginUpload(bitmap = newPhoto, imageData = byteArrayOf(1)).join()

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Failure>()
            viewModel.avatarSource.value shouldBe previousSource

            viewModel.retryUpload().join()

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Loaded<Unit>>()
            viewModel.avatarSource.value shouldBe ProfileAvatarSource.Local(newPhoto)
            coVerify(exactly = 2) { uploadAvatar.execute(any()) }
        }

    @Test
    fun `given an upload succeeds then the avatar version bumps so navigation chrome refetches`() =
        runTest {
            val uploadAvatar = mockk<UploadAvatar>()
            coEvery { uploadAvatar.execute(any()) } returns Result.success(Unit)
            val viewModel = ProfileViewModel(
                uploadAvatar = uploadAvatar,
                accountStorage = FakeAccountStorage(bundle = storedBundle),
            )
            val versionBefore = AvatarVersion.value.value

            viewModel.beginUpload(bitmap = mockk(), imageData = byteArrayOf(1)).join()

            AvatarVersion.value.value shouldBe versionBefore + 1
        }

    @Test
    fun `given an upload fails then the avatar version does not bump`() = runTest {
        val uploadAvatar = mockk<UploadAvatar>()
        coEvery { uploadAvatar.execute(any()) } returns
            Result.failure(RuntimeException("upload failed"))
        val viewModel = ProfileViewModel(
            uploadAvatar = uploadAvatar,
            accountStorage = FakeAccountStorage(bundle = storedBundle),
        )
        val versionBefore = AvatarVersion.value.value

        viewModel.beginUpload(bitmap = mockk(), imageData = byteArrayOf(1)).join()

        AvatarVersion.value.value shouldBe versionBefore
    }

    @Test
    fun `given the upload failed when the error is dismissed then the pending photo is discarded`() =
        runTest {
            val uploadAvatar = mockk<UploadAvatar>()
            coEvery { uploadAvatar.execute(any()) } returns
                Result.failure(RuntimeException("upload failed"))
            val viewModel = ProfileViewModel(
                uploadAvatar = uploadAvatar,
                accountStorage = FakeAccountStorage(bundle = storedBundle),
            )
            val previousSource = viewModel.avatarSource.value

            viewModel.beginUpload(bitmap = mockk(), imageData = byteArrayOf(1)).join()
            viewModel.dismissUploadError()

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Idle>()
            viewModel.avatarSource.value shouldBe previousSource
        }
}

private val storedBundle = AccountBundle(
    accountId = "account-1",
    externalAccountId = "external-1",
    email = "gambler@tyche.com",
).withUsername("ElGoleador")

private class FakeAccountStorage(bundle: AccountBundle?) : AccountStorage {
    private val _state = MutableStateFlow(bundle)
    override val state: StateFlow<AccountBundle?> = _state.asStateFlow()

    override suspend fun store(accountBundle: AccountBundle) {
        _state.value = accountBundle
    }

    override suspend fun delete() {
        _state.value = null
    }

    override suspend fun retrieve(): AccountBundle? = _state.value
}
