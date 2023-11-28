package com.felipearpa.tyche.ui.state

sealed class EditableViewState<out Value : Any> {
    data class Initial<Value : Any>(val value: Value) : EditableViewState<Value>()

    data class Loading<Value : Any>(val current: Value, val target: Value) :
        EditableViewState<Value>()

    data class Success<Value : Any>(val old: Value, val succeeded: Value) :
        EditableViewState<Value>()

    data class Failure<Value : Any>(
        val current: Value,
        val failed: Value,
        val exception: Throwable
    ) : EditableViewState<Value>()
}

fun <Value : Any> EditableViewState<Value>.isInitial() = this is EditableViewState.Initial

fun <Value : Any> EditableViewState<Value>.isLoading() = this is EditableViewState.Loading

fun <Value : Any> EditableViewState<Value>.isSuccess() = this is EditableViewState.Success

fun <Value : Any> EditableViewState<Value>.isFailure() = this is EditableViewState.Failure

fun <Value : Any> EditableViewState<Value>.exceptionOrNull(): Throwable? {
    return when (this) {
        is EditableViewState.Failure -> this.exception
        else -> null
    }
}

fun <Value : Any> EditableViewState<Value>.currentValue() =
    when (this) {
        is EditableViewState.Initial -> this.value
        is EditableViewState.Loading -> this.current
        is EditableViewState.Success -> this.succeeded
        is EditableViewState.Failure -> this.current
    }