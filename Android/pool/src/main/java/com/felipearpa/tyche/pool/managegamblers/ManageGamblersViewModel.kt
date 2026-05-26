package com.felipearpa.tyche.pool.managegamblers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.data.pool.application.GetPoolMembers
import com.felipearpa.tyche.data.pool.application.RemoveGambler
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.MutationState
import com.felipearpa.ui.state.activeValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class ManageGamblersViewModel(
    private val poolId: String,
    private val getPoolMembers: GetPoolMembers,
    private val removeGambler: RemoveGambler,
) : ViewModel() {

    val pageSize = PAGE_SIZE

    val poolMembers = buildPager().flow
        .cachedIn(scope = viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PagingData.empty(),
        )

    private val _removalStates = MutableStateFlow<Map<String, MutationState<PoolMemberModel>>>(emptyMap())

    val deletingGamblerIds: StateFlow<Set<String>> = _removalStates
        .map { states -> states.filterValues { state -> state is MutationState.Mutating }.keys }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val removedGamblerIds: StateFlow<Set<String>> = _removalStates
        .map { states -> states.filterValues { state -> state is MutationState.Mutated }.keys }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val failedGambler: StateFlow<PoolMemberModel?> = _removalStates
        .map { states -> states.values.firstOrNull { state -> state is MutationState.Failure }?.activeValue() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private fun buildPager() =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                PoolMemberPagingSource(
                    pagingQuery = { next ->
                        getPoolMembersPagingQuery(
                            next = next,
                            poolId = poolId,
                            getPoolMembers = getPoolMembers,
                        )
                    },
                )
            },
        )

    fun remove(member: PoolMemberModel) {
        viewModelScope.launch {
            override(member.gamblerId, MutationState.Mutating(original = member, updated = member))

            removeGambler.execute(poolId = poolId, gamblerId = member.gamblerId)
                .onSuccess {
                    override(member.gamblerId, MutationState.Mutated(original = member, updated = member))
                }
                .onFailure { exception ->
                    override(
                        member.gamblerId,
                        MutationState.Failure(
                            original = member,
                            updated = member,
                            exception = exception.orDefaultLocalized(),
                        ),
                    )
                }
        }
    }

    fun dismissFailure() {
        _removalStates.update { states -> states.filterValues { state -> state !is MutationState.Failure } }
    }

    private fun override(gamblerId: String, state: MutationState<PoolMemberModel>) {
        _removalStates.update { states -> states + (gamblerId to state) }
    }
}
