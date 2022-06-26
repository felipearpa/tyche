package com.pipel.tyche.poolGambler.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pipel.core.empty
import com.pipel.tyche.poolGambler.useCase.FindPoolsGamblersUseCase
import com.pipel.tyche.poolGambler.view.PoolGamblerModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PoolGamblerViewModel @AssistedInject constructor(
    private val findPoolsGamblersUseCase: FindPoolsGamblersUseCase,
    @Assisted private val poolId: String
) :
    ViewModel() {

    val pageSize = 50
    private val prefetchDistance = 5

    private var poolGamblerPagingSource: PoolGamblerPagingSource? = null

    private val _filterTextFlow = MutableStateFlow(String.empty())
    val filterTextFlow: StateFlow<String>
        get() = _filterTextFlow.asStateFlow()

    private var _poolsGamblersFlow = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            PoolGamblerPagingSource(
                PoolGamblerPagingQuery(
                    poolId = poolId,
                    findPoolsGamblersUseCase = findPoolsGamblersUseCase,
                    filterFunc = { _filterTextFlow.value }
                )
            ).also {
                this.poolGamblerPagingSource = it
            }
        }
    ).flow
    val poolsGamblersFlow: Flow<PagingData<PoolGamblerModel>>
        get() = _poolsGamblersFlow

    init {
        viewModelScope.launch {
            _filterTextFlow.collectLatest {
                poolGamblerPagingSource?.invalidate()
            }
        }
    }

    fun applyFilter(filterText: String) {
        _filterTextFlow.value = filterText
    }

}