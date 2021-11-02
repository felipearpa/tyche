package com.pipel.tyche.ui.poollayout

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pipel.core.empty
import com.pipel.tyche.usecase.FindPoolsLayoutsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PoolLayoutViewModel @Inject constructor(private val findPoolsLayoutsUseCase: FindPoolsLayoutsUseCase) :
    ViewModel() {

    private var poolLayoutPagingSource: PoolLayoutPagingSource? = null

    private val _filterTextFlow = MutableStateFlow(String.empty())
    val filterTextFlow: StateFlow<String>
        get() = _filterTextFlow.asStateFlow()

    private var _poolsLayoutsFlow = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = true),
        pagingSourceFactory = {
            PoolLayoutPagingSource(
                PoolLayoutPagingQuery(
                    findPoolsLayoutsUseCase
                ) { _filterTextFlow.value }
            ).also { poolLayoutPagingSource ->
                this.poolLayoutPagingSource = poolLayoutPagingSource
            }
        }
    ).flow
    val poolsLayoutsFlow: Flow<PagingData<PoolLayoutModel>>
        get() = _poolsLayoutsFlow

    fun onFilterChange(filter: String) {
        _filterTextFlow.value = filter
        poolLayoutPagingSource?.invalidate()
    }

    companion object {
        const val PAGE_SIZE = 50
        private const val PREFETCH_DISTANCE = 5
    }

}