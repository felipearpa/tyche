package com.felipearpa.tyche.poolscore.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.authentication.application.LogOutUseCase
import kotlinx.coroutines.launch

class DrawerViewModel(private val logoutUseCase: LogOutUseCase) : ViewModel() {
    fun logout() {
        viewModelScope.launch {
            logoutUseCase.execute()
        }
    }
}
