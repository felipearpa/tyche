package com.felipearpa.tyche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.authentication.application.UpdateUsername
import com.felipearpa.ui.state.SaveState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsernameEditorViewModel(
    private val updateUsername: UpdateUsername,
) : ViewModel() {
    private val _saveState =
        MutableStateFlow<SaveState<String>>(SaveState.Idle)
    val saveState: StateFlow<SaveState<String>> = _saveState.asStateFlow()

    private var lastAttempt: String? = null

    fun save(newUsername: String) {
        if (_saveState.value is SaveState.Saving) return
        val trimmed = newUsername.trim()
        if (trimmed.isEmpty()) return
        lastAttempt = trimmed
        performSave(trimmed)
    }

    fun retry() {
        val attempt = lastAttempt ?: return
        if (_saveState.value is SaveState.Saving) return
        performSave(attempt)
    }

    fun resetError() {
        if (_saveState.value is SaveState.Failure) {
            _saveState.value = SaveState.Idle
        }
    }

    fun reset() {
        _saveState.value = SaveState.Idle
        lastAttempt = null
    }

    private fun performSave(username: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving(username)
            updateUsername.execute(username)
                .onSuccess { saved -> _saveState.value = SaveState.Saved(saved) }
                .onFailure { exception -> _saveState.value = SaveState.Failure(username, exception) }
        }
    }
}
