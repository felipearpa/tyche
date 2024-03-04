package com.felipearpa.tyche.ui.state

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class LoadableViewStateTest {
    @Test
    fun `given a state of initial when checked if initial then initial is confirmed`() {
        val viewState = LoadableViewState.Initial
        val isInitial = viewState.isInitial()
        isInitial.shouldBeTrue()
    }

    @TestFactory
    fun `given a state of no initial when checked if initial then initial is not confirmed`() =
        listOf(
            LoadableViewState.Loading,
            LoadableViewState.Failure(RuntimeException()),
            LoadableViewState.Success(Unit)
        ).map { viewState ->
            dynamicTest("given a state of $viewState when checked if initial then initial is not confirmed") {
                val isInitial = viewState.isInitial()
                isInitial.shouldBeFalse()
            }
        }

    @Test
    fun `given a state of loading when checked if loading then loading is confirmed`() {
        val viewState = LoadableViewState.Loading
        val isLoading = viewState.isLoading()
        isLoading.shouldBeTrue()
    }

    @TestFactory
    fun `given a state of no loading when checked if loading then loading is not confirmed`() =
        listOf(
            LoadableViewState.Initial,
            LoadableViewState.Failure(RuntimeException()),
            LoadableViewState.Success(Unit)
        ).map { viewState ->
            dynamicTest("given a state of $viewState when checked if loading then loading is not confirmed") {
                val isLoading = viewState.isLoading()
                isLoading.shouldBeFalse()
            }
        }

    @Test
    fun `given a state of failure when checked if failure then failure is confirmed`() {
        val viewState = LoadableViewState.Failure(RuntimeException())
        val isFailure = viewState.isFailure()
        isFailure.shouldBeTrue()
    }

    @TestFactory
    fun `given a state of no failure when checked if failure then failure is not confirmed`() =
        listOf(
            LoadableViewState.Initial,
            LoadableViewState.Loading,
            LoadableViewState.Success(Unit)
        ).map { viewState ->
            dynamicTest("given a state of $viewState when checked if failure then failure is not confirmed") {
                val isFailure = viewState.isFailure()
                isFailure.shouldBeFalse()
            }
        }

    @Test
    fun `given a state of success when checked if success then success is confirmed`() {
        val viewState = LoadableViewState.Success(Unit)
        val isSuccess = viewState.isSuccess()
        isSuccess.shouldBeTrue()
    }

    @TestFactory
    fun `given a state of no success when checked if success then success is not confirmed`() =
        listOf(
            LoadableViewState.Initial,
            LoadableViewState.Loading,
            LoadableViewState.Failure(RuntimeException())
        ).map { viewState ->
            dynamicTest("given a state of $viewState when checked if success then success is not confirmed") {
                val isSuccess = viewState.isSuccess()
                isSuccess.shouldBeFalse()
            }
        }

    @Test
    fun `given a failure when checked for exception then the exception is found`() {
        val viewState = LoadableViewState.Failure(RuntimeException())
        val exception = viewState.exceptionOrNull()
        exception.shouldBeInstanceOf<RuntimeException>()
    }

    @TestFactory
    fun `given a no failure when checked for exception then the exception is not found`() =
        listOf(
            LoadableViewState.Initial,
            LoadableViewState.Loading,
            LoadableViewState.Success(Unit)
        ).map { viewState ->
            dynamicTest("given a $viewState when checked for exception then the exception is not found") {
                val exception = viewState.exceptionOrNull()
                exception.shouldBeNull()
            }
        }
}