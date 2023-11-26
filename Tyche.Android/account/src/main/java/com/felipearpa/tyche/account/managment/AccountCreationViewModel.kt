package com.felipearpa.tyche.account.managment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.data.account.AccountBundle
import com.felipearpa.data.account.managment.application.CreateAccountUseCase
import com.felipearpa.tyche.ui.LoadableViewState
import com.felipearpa.tyche.ui.toLocalizedException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountCreationViewModel @Inject constructor(
    private val createUserUseCase: CreateAccountUseCase
) : ViewModel() {

    private val _state =
        MutableStateFlow<LoadableViewState<AccountBundle>>(LoadableViewState.Initial)
    val state: Flow<LoadableViewState<AccountBundle>>
        get() = _state.asStateFlow()

    fun reset() {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Initial)
        }
    }

    fun create(account: AccountModel) {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)
            createUserUseCase.execute(account.toAccount())
                .onSuccess { accountBundle -> _state.emit(LoadableViewState.Success(accountBundle)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadableViewState.Failure(
                            exception.toAccountCreationLocalizedExceptionOnMatch()
                                .toLocalizedException()
                        )
                    )
                }
        }
    }
}