package com.felipearpa.tyche.profile

import android.graphics.Bitmap
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.CurrentAccountRefreshTrigger
import com.felipearpa.tyche.session.avatar.application.UploadAvatar
import com.felipearpa.ui.state.LoadState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `given no published account when created then the letter avatar is shown`() = runTest {
        val viewModel = ProfileViewModel(
            uploadAvatar = mockk(),
            currentAccountCoordinator = coordinatorPublishing(bundle = null),
        )

        viewModel.avatarSource.value shouldBe ProfileAvatarSource.Letter
    }

    @Test
    fun `given a published account when created then the avatar url derives from the account id and a profile refresh is requested`() =
        runTest {
            val coordinator = coordinatorPublishing(bundle = storedBundle)

            val viewModel = ProfileViewModel(
                uploadAvatar = mockk(),
                currentAccountCoordinator = coordinator,
            )

            viewModel.avatarSource.value shouldBe
                ProfileAvatarSource.Remote(
                    "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg",
                )
            viewModel.username.value shouldBe "ElGoleador"
            viewModel.email.value shouldBe "gambler@tyche.com"
            coVerify(exactly = 1) {
                coordinator.refresh(CurrentAccountRefreshTrigger.PROFILE_OPENED)
            }
        }

    @Test
    fun `given an upload begins then the new photo shows immediately and success returns to the remote source as loaded`() =
        runTest {
            val uploadAvatar = mockk<UploadAvatar>()
            val gate = CompletableDeferred<Unit>()
            coEvery { uploadAvatar.execute(any()) } coAnswers {
                gate.await()
                Result.success(Unit)
            }
            val viewModel = ProfileViewModel(
                uploadAvatar = uploadAvatar,
                currentAccountCoordinator = coordinatorPublishing(bundle = storedBundle),
            )
            val newPhoto = mockk<Bitmap>(relaxed = true)
            val imageData = byteArrayOf(9, 9)

            val job = viewModel.beginUpload(bitmap = newPhoto, imageData = imageData)

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Loading>()
            viewModel.avatarSource.value shouldBe ProfileAvatarSource.Local(newPhoto)

            gate.complete(Unit)
            job.join()

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Loaded<Unit>>()
            // After success the remote branch is served from the store that the upload flow
            // seeded; the view model retains no local copy.
            viewModel.avatarSource.value shouldBe
                ProfileAvatarSource.Remote(
                    "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg",
                )
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
                currentAccountCoordinator = coordinatorPublishing(bundle = storedBundle),
            )
            val previousSource = viewModel.avatarSource.value
            val newPhoto = mockk<Bitmap>(relaxed = true)

            viewModel.beginUpload(bitmap = newPhoto, imageData = byteArrayOf(1)).join()

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Failure>()
            viewModel.avatarSource.value shouldBe previousSource

            viewModel.retryUpload().join()

            viewModel.uploadState.value.shouldBeInstanceOf<LoadState.Loaded<Unit>>()
            coVerify(exactly = 2) { uploadAvatar.execute(any()) }
        }

    @Test
    fun `given the upload failed when the error is dismissed then the pending photo is discarded`() =
        runTest {
            val uploadAvatar = mockk<UploadAvatar>()
            coEvery { uploadAvatar.execute(any()) } returns
                Result.failure(RuntimeException("upload failed"))
            val viewModel = ProfileViewModel(
                uploadAvatar = uploadAvatar,
                currentAccountCoordinator = coordinatorPublishing(bundle = storedBundle),
            )
            val previousSource = viewModel.avatarSource.value

            viewModel.beginUpload(bitmap = mockk(relaxed = true), imageData = byteArrayOf(1))
                .join()
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

private fun coordinatorPublishing(bundle: AccountBundle?): CurrentAccountCoordinator =
    mockk<CurrentAccountCoordinator> {
        every { state } returns MutableStateFlow(bundle)
        coJustRun { refresh(any()) }
    }
