package com.felipearpa.tyche.pool.managegamblers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.data.pool.application.GetPoolMembers
import com.felipearpa.tyche.data.pool.application.RemoveGambler
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

    private val _deletingGamblerIds = MutableStateFlow<Set<String>>(emptySet())
    val deletingGamblerIds = _deletingGamblerIds.asStateFlow()

    private val _removedGamblerIds = MutableStateFlow<Set<String>>(emptySet())
    val removedGamblerIds = _removedGamblerIds.asStateFlow()

    private val _failedGambler = MutableStateFlow<PoolMemberModel?>(null)
    val failedGambler = _failedGambler.asStateFlow()

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
            _failedGambler.value = null
            _deletingGamblerIds.update { ids -> ids + member.gamblerId }

            removeGambler.execute(poolId = poolId, gamblerId = member.gamblerId)
                .onSuccess {
                    _deletingGamblerIds.update { ids -> ids - member.gamblerId }
                    _removedGamblerIds.update { ids -> ids + member.gamblerId }
                }
                .onFailure {
                    _deletingGamblerIds.update { ids -> ids - member.gamblerId }
                    _failedGambler.value = member
                }
        }
    }

    fun dismissFailure() {
        _failedGambler.value = null
    }
}
