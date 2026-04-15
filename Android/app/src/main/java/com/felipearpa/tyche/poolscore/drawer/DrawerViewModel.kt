package com.felipearpa.tyche.poolscore.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.authentication.application.LogOut
import kotlinx.coroutines.launch

class DrawerViewModel(private val logOut: LogOut) : ViewModel() {
    fun logout() {
        viewModelScope.launch {
            logOut.execute()
        }
    }
}
