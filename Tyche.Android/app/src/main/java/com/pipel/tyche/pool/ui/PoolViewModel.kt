package com.pipel.tyche.pool.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pipel.core.empty
import com.pipel.tyche.pool.useCase.FindPoolsUseCase
import com.pipel.tyche.pool.view.PoolModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PoolViewModel @AssistedInject constructor(
    private val findPoolsUseCase: FindPoolsUseCase,
    @Assisted private val pooLayoutId: String
) :
    ViewModel() {

    val pageSize = 50
    private val prefetchDistance = 5

    private var poolPagingSource: PoolPagingSource? = null

    private val _filterTextFlow = MutableStateFlow(String.empty())
    val filterTextFlow: StateFlow<String>
        get() = _filterTextFlow.asStateFlow()

    private var _poolsFlow = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            PoolPagingSource(
                PoolPagingQuery(
                    poolLayoutId = pooLayoutId,
                    findPoolsUseCase = findPoolsUseCase,
                    filterFunc = { _filterTextFlow.value }
                )
            ).also {
                this.poolPagingSource = it
            }
        }
    ).flow
    val poolsFlow: Flow<PagingData<PoolModel>>
        get() = _poolsFlow

    init {
        viewModelScope.launch {
            _filterTextFlow.collectLatest {
                poolPagingSource?.invalidate()
            }
        }
    }

    fun applyFilter(filterText: String) {
        _filterTextFlow.value = filterText
    }

}