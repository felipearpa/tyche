package com.felipearpa.tyche.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.authentication.application.LogOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val logoutUseCase: LogOutUseCase) :
    ViewModel() {
    fun logout() {
        viewModelScope.launch {
            logoutUseCase.execute()
        }
    }
}