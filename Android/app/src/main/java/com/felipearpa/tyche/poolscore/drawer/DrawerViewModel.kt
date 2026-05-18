package com.felipearpa.tyche.poolscore.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.application.LogOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrawerViewModel(
    private val logOut: LogOut,
    private val accountStorage: AccountStorage,
) : ViewModel() {
    private val _email = MutableStateFlow(emptyString())
    val email: StateFlow<String> = _email.asStateFlow()

    private val _username = MutableStateFlow(emptyString())
    val username: StateFlow<String> = _username.asStateFlow()

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

    fun applyUsername(newUsername: String) {
        _username.value = newUsername
    }
}
