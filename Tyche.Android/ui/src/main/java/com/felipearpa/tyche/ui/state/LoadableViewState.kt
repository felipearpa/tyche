package com.felipearpa.tyche.ui.state

sealed class LoadableViewState<out Value : Any> {
    data object Initial : LoadableViewState<Nothing>()

    data object Loading : LoadableViewState<Nothing>()

    data class Success<Value : Any>(val value: Value) : LoadableViewState<Value>() {
        operator fun invoke(): Value = value
    }

    data class Failure(val exception: Throwable) : LoadableViewState<Nothing>() {
        operator fun invoke(): Throwable = exception
    }
}

fun <Value : Any> LoadableViewState<Value>.isInitial() = this is LoadableViewState.Initial

fun <Value : Any> LoadableViewState<Value>.isLoading() = this is LoadableViewState.Loading

fun <Value : Any> LoadableViewState<Value>.isSuccess() = this is LoadableViewState.Success

fun <Value : Any> LoadableViewState<Value>.isFailure() = this is LoadableViewState.Failure

inline fun <T : Any> LoadableViewState<T>.onInitial(block: () -> Unit): LoadableViewState<T> {
    if (isInitial()) {
        block()
    }
    return this
}

inline fun <Value : Any> LoadableViewState<Value>.onLoading(block: () -> Unit): LoadableViewState<Value> {
    if (isLoading()) {
        block()
    }
    return this
}

inline fun <Value : Any> LoadableViewState<Value>.onSuccess(block: (value: Value) -> Unit): LoadableViewState<Value> {
    if (isSuccess()) {
        block((this as LoadableViewState.Success).invoke())
    }
    return this
}

inline fun <Value : Any> LoadableViewState<Value>.onFailure(block: (throwable: Throwable) -> Unit): LoadableViewState<Value> {
    if (isFailure()) {
        block((this as LoadableViewState.Failure).invoke())
    }
    return this
}

fun <Value : Any> LoadableViewState<Value>.valueOrNull(): Value? {
    if (isSuccess())
        return (this as LoadableViewState.Success).invoke()
    return null
}

fun <Value : Any> LoadableViewState<Value>.exceptionOrNull(): Throwable? {
    if (isFailure())
        return (this as LoadableViewState.Failure).invoke()
    return null
}