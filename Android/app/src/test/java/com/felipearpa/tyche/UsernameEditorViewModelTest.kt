package com.felipearpa.tyche

import com.felipearpa.tyche.session.authentication.application.UpdateUsername
import com.felipearpa.ui.state.SaveState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UsernameEditorViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a changed username when saved then the state moves through saving to saved`() = runTest {
        val updateUsername = mockk<UpdateUsername>()
        val gate = CompletableDeferred<Result<String>>()
        coEvery { updateUsername.execute("neptune") } coAnswers { gate.await() }

        val viewModel = UsernameEditorViewModel(updateUsername = updateUsername)

        viewModel.save("neptune")
        viewModel.saveState.value shouldBe SaveState.Saving("neptune")

        gate.complete(Result.success("neptune"))
        viewModel.saveState.value shouldBe SaveState.Saved("neptune")
    }

    @Test
    fun `given a save when it fails then the state is failure carrying the attempted value`() = runTest {
        val updateUsername = mockk<UpdateUsername>()
        coEvery { updateUsername.execute("neptune") } returns Result.failure(RuntimeException("boom"))

        val viewModel = UsernameEditorViewModel(updateUsername = updateUsername)

        viewModel.save("neptune")

        val state = viewModel.saveState.value
        state.shouldBeInstanceOf<SaveState.Failure<String>>()
        state.value shouldBe "neptune"
    }

    @Test
    fun `given a failed save when retried then the same pending value is resubmitted`() = runTest {
        val updateUsername = mockk<UpdateUsername>()
        coEvery { updateUsername.execute("neptune") } returns Result.failure(RuntimeException("boom"))

        val viewModel = UsernameEditorViewModel(updateUsername = updateUsername)

        viewModel.save("neptune")
        viewModel.saveState.value.shouldBeInstanceOf<SaveState.Failure<String>>()

        coEvery { updateUsername.execute("neptune") } returns Result.success("neptune")
        viewModel.retry()

        viewModel.saveState.value shouldBe SaveState.Saved("neptune")
        coVerify(exactly = 2) { updateUsername.execute("neptune") }
    }

    @Test
    fun `given a failure when the error is reset then the state returns to idle`() = runTest {
        val updateUsername = mockk<UpdateUsername>()
        coEvery { updateUsername.execute("neptune") } returns Result.failure(RuntimeException("boom"))

        val viewModel = UsernameEditorViewModel(updateUsername = updateUsername)

        viewModel.save("neptune")
        viewModel.saveState.value.shouldBeInstanceOf<SaveState.Failure<String>>()

        viewModel.resetError()

        viewModel.saveState.value shouldBe SaveState.Idle
    }

    @Test
    fun `given a blank draft when saved then no request is made`() = runTest {
        val updateUsername = mockk<UpdateUsername>()

        val viewModel = UsernameEditorViewModel(updateUsername = updateUsername)

        viewModel.save("   ")

        viewModel.saveState.value shouldBe SaveState.Idle
        coVerify(exactly = 0) { updateUsername.execute(any()) }
    }

    @Test
    fun `given an in-flight save when save is called again then it is ignored`() = runTest {
        val updateUsername = mockk<UpdateUsername>()
        val gate = CompletableDeferred<Result<String>>()
        coEvery { updateUsername.execute("neptune") } coAnswers { gate.await() }

        val viewModel = UsernameEditorViewModel(updateUsername = updateUsername)

        viewModel.save("neptune")
        viewModel.saveState.value shouldBe SaveState.Saving("neptune")

        viewModel.save("other")

        gate.complete(Result.success("neptune"))
        viewModel.saveState.value shouldBe SaveState.Saved("neptune")
        coVerify(exactly = 0) { updateUsername.execute("other") }
    }

    @Test
    fun `given a completed save when reset then the state returns to idle`() = runTest {
        val updateUsername = mockk<UpdateUsername>()
        coEvery { updateUsername.execute("neptune") } returns Result.success("neptune")

        val viewModel = UsernameEditorViewModel(updateUsername = updateUsername)

        viewModel.save("neptune")
        viewModel.saveState.value shouldBe SaveState.Saved("neptune")

        viewModel.reset()

        viewModel.saveState.value shouldBe SaveState.Idle
    }
}
