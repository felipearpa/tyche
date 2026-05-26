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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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
    val removalStates = _removalStates.asStateFlow()

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
