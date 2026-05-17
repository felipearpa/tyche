package com.felipearpa.tyche.poolscore.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.application.LogOut
import com.felipearpa.tyche.session.authentication.application.UpdateUsername
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrawerViewModel(
    private val logOut: LogOut,
    private val updateUsername: UpdateUsername,
    private val accountStorage: AccountStorage,
) : ViewModel() {
    private val _email = MutableStateFlow(emptyString())
    val email: StateFlow<String> = _email.asStateFlow()

    private val _username = MutableStateFlow(emptyString())
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isSavingUsername = MutableStateFlow(false)
    val isSavingUsername: StateFlow<Boolean> = _isSavingUsername.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError.asStateFlow()

    init {
        val bundle = accountStorage.state.value
        _email.value = bundle?.email.orEmpty()
        _username.value = bundle?.username.orEmpty()
    }

    fun logout() {
        viewModelScope.launch {
            logOut.execute()
        }
    }

    fun changeUsername(newUsername: String, onSaved: () -> Unit = {}) {
        if (_isSavingUsername.value) return
        val trimmed = newUsername.trim()
        if (trimmed.isEmpty() || trimmed == _username.value) {
            onSaved()
            return
        }

        viewModelScope.launch {
            _isSavingUsername.value = true
            _usernameError.value = null
            updateUsername.execute(trimmed)
                .onSuccess { saved ->
                    _username.value = saved
                    onSaved()
                }
                .onFailure { _usernameError.value = USERNAME_UPDATE_FAILED }
            _isSavingUsername.value = false
        }
    }

    fun clearUsernameError() {
        _usernameError.value = null
    }

    companion object {
        const val USERNAME_UPDATE_FAILED = "USERNAME_UPDATE_FAILED"
    }
}
