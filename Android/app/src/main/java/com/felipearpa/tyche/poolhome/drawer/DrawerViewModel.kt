package com.felipearpa.tyche.poolhome.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.data.pool.application.DeletePool
import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.toPoolGamblerScoreModel
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.application.LogOut
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.LoadState
import com.felipearpa.ui.state.SaveState
import com.felipearpa.ui.state.isSaving
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrawerViewModel(
    val poolId: String,
    val gamblerId: String,
    private val logOut: LogOut,
    private val getPoolGamblerScore: GetPoolGamblerScore,
    private val getPool: GetPool,
    private val deletePool: DeletePool,
    accountStorage: AccountStorage,
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadState<PoolGamblerScoreModel>>(LoadState.Idle)
    val state = _state.asStateFlow()

    val email: StateFlow<String> = accountStorage.state
        .map { bundle -> bundle?.email.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = accountStorage.state.value?.email.orEmpty(),
        )

    val username: StateFlow<String> = accountStorage.state
        .map { bundle -> bundle?.username.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = accountStorage.state.value?.username.orEmpty(),
        )

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _gamblerCount = MutableStateFlow<Int?>(null)
    val gamblerCount: StateFlow<Int?> = _gamblerCount.asStateFlow()

    private val _deleteState =
        MutableStateFlow<SaveState<Unit>>(SaveState.Idle)
    val deleteState = _deleteState.asStateFlow()

    private fun buildUiState(
        email: String,
        username: String,
        scoreState: LoadState<PoolGamblerScoreModel>,
        isOwner: Boolean,
        gamblerCount: Int?,
        deleteState: SaveState<Unit>,
    ) = PoolHomeDrawerUiState(
        accountId = gamblerId,
        email = email,
        username = username,
        poolGamblerScoreState = scoreState,
        isOwner = isOwner,
        gamblerCount = gamblerCount,
        isDeleting = deleteState.isSaving(),
    )

    val uiState: StateFlow<PoolHomeDrawerUiState> = combine(
        combine(email, username) { email, username -> email to username },
        state,
        isOwner,
        gamblerCount,
        deleteState,
    ) { (email, username), scoreState, isOwner, gamblerCount, deleteState ->
        buildUiState(email, username, scoreState, isOwner, gamblerCount, deleteState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildUiState(
            email = email.value,
            username = username.value,
            scoreState = state.value,
            isOwner = isOwner.value,
            gamblerCount = gamblerCount.value,
            deleteState = deleteState.value,
        ),
    )

    init {
        viewModelScope.launch {
            _state.emit(LoadState.Loading)

            val poolResult =
                getPoolGamblerScore.execute(poolId = poolId, gamblerId = gamblerId)
            poolResult.onSuccess { poolGamblerScore ->
                _state.emit(LoadState.Loaded(poolGamblerScore.toPoolGamblerScoreModel()))
            }.onFailure { exception ->
                _state.emit(LoadState.Failure(exception.orDefaultLocalized()))
            }
        }
    }

    init {
        viewModelScope.launch {
            getPool.execute(poolId = poolId).onSuccess { pool ->
                _isOwner.value = pool.creatorGamblerId == gamblerId
                _gamblerCount.value = pool.gamblerCount
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logOut.execute()
        }
    }

    fun deletePool(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _deleteState.emit(SaveState.Saving(Unit))

            deletePool.execute(poolId = poolId, gamblerId = gamblerId)
                .onSuccess {
                    _deleteState.emit(SaveState.Saved(Unit))
                    onSuccess()
                }
                .onFailure { exception ->
                    _deleteState.emit(SaveState.Failure(Unit, exception.orDefaultLocalized()))
                }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = SaveState.Idle
    }
}
