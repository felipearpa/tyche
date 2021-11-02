package com.pipel.ui.coroutine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

fun ViewModel.launchAsync(block: suspend () -> Unit) {
    viewModelScope.launch {
        block()
    }
}