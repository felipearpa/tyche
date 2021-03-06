package com.pipel.tyche.poolLayout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pipel.core.empty
import com.pipel.tyche.poolLayout.useCase.FindActivePoolsLayoutsUseCase
import com.pipel.tyche.poolLayout.view.PoolLayoutModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoolLayoutViewModel @Inject constructor(private val findActivePoolsLayoutsUseCase: FindActivePoolsLayoutsUseCase) :
    ViewModel() {

    val pageSize = 50
    private val prefetchDistance = 5

    private var poolLayoutPagingSource: PoolLayoutPagingSource? = null

    private val _filterTextFlow = MutableStateFlow(String.empty())
    val filterTextFlow: StateFlow<String>
        get() = _filterTextFlow.asStateFlow()

    private var _poolsLayoutsFlow = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            PoolLayoutPagingSource(
                PoolLayoutPagingQuery(
                    findActivePoolsLayoutsUseCase
                ) { _filterTextFlow.value }
            ).also {
                this.poolLayoutPagingSource = it
            }
        }
    ).flow
    val poolsLayoutsFlow: Flow<PagingData<PoolLayoutModel>>
        get() = _poolsLayoutsFlow

    init {
        viewModelScope.launch {
            _filterTextFlow.collectLatest {
                poolLayoutPagingSource?.invalidate()
            }
        }
    }

    fun applyFilter(filterText: String) {
        _filterTextFlow.value = filterText
    }

}