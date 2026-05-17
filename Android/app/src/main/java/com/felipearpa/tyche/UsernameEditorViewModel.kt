package com.felipearpa.tyche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.authentication.application.UpdateUsername
import com.felipearpa.ui.state.LoadableViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsernameEditorViewModel(
    private val updateUsername: UpdateUsername,
) : ViewModel() {
    private val _saveState =
        MutableStateFlow<LoadableViewState<String>>(LoadableViewState.Initial)
    val saveState: StateFlow<LoadableViewState<String>> = _saveState.asStateFlow()

    private var lastAttempt: String? = null

    fun save(newUsername: String) {
        if (_saveState.value is LoadableViewState.Loading) return
        val trimmed = newUsername.trim()
        if (trimmed.isEmpty()) return
        lastAttempt = trimmed
        performSave(trimmed)
    }

    fun retry() {
        val attempt = lastAttempt ?: return
        if (_saveState.value is LoadableViewState.Loading) return
        performSave(attempt)
    }

    fun resetError() {
        if (_saveState.value is LoadableViewState.Failure) {
            _saveState.value = LoadableViewState.Initial
        }
    }

    fun reset() {
        _saveState.value = LoadableViewState.Initial
        lastAttempt = null
    }

    private fun performSave(username: String) {
        viewModelScope.launch {
            _saveState.value = LoadableViewState.Loading
            updateUsername.execute(username)
                .onSuccess { saved -> _saveState.value = LoadableViewState.Success(saved) }
                .onFailure { exception -> _saveState.value = LoadableViewState.Failure(exception) }
        }
    }
}
