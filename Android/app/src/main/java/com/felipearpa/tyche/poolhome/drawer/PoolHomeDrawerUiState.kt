package com.felipearpa.tyche.poolhome.drawer

import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.ui.state.LoadState

data class PoolHomeDrawerUiState(
    val accountId: String,
    val email: String,
    val username: String,
    val poolGamblerScoreState: LoadState<PoolGamblerScoreModel>,
    val isOwner: Boolean,
    val gamblerCount: Int?,
    val isDeleting: Boolean,
)
