package com.felipearpa.ui

sealed class ViewState<out T : Any> {

    object Initial : ViewState<Nothing>()

    object Loading : ViewState<Nothing>()

    data class Success<T : Any>(private val value: T) : ViewState<T>() {
        operator fun invoke(): T = value
    }

    data class Failure(private val throwable: Throwable) : ViewState<Nothing>() {
        operator fun invoke(): Throwable = throwable
    }
}

fun <T : Any> ViewState<T>.isInitial() = this is ViewState.Initial

fun <T : Any> ViewState<T>.isLoading() = this is ViewState.Loading

fun <T : Any> ViewState<T>.isSuccess() = this is ViewState.Success

fun <T : Any> ViewState<T>.isFailure() = this is ViewState.Failure

inline fun <T : Any> ViewState<T>.onInitial(block: () -> Unit): ViewState<T> {
    if (isInitial()) {
        block()
    }
    return this
}

inline fun <T : Any> ViewState<T>.onLoading(block: () -> Unit): ViewState<T> {
    if (isLoading()) {
        block()
    }
    return this
}

inline fun <T : Any> ViewState<T>.onSuccess(block: (value: T) -> Unit): ViewState<T> {
    if (isSuccess()) {
        block((this as ViewState.Success).invoke())
    }
    return this
}

inline fun <T : Any> ViewState<T>.onFailure(block: (throwable: Throwable) -> Unit): ViewState<T> {
    if (isFailure()) {
        block((this as ViewState.Failure).invoke())
    }
    return this
}