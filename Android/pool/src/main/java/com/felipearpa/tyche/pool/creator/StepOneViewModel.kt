package com.felipearpa.tyche.pool.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.data.pool.application.GetOpenPoolLayoutsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

private const val PAGE_SIZE = 5
private const val PREFETCH_DISTANCE = 5

@HiltViewModel
internal class StepOneViewModel @Inject constructor(
    private val getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase,
) : ViewModel() {
    val pageSize = PAGE_SIZE

    val poolLayouts = buildPager(searchText = emptyString()).flow.cachedIn(viewModelScope).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = PagingData.empty(),
    )

    private fun buildPager(searchText: String) =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = {
                PoolLayoutPagingSource(
                    pagingQuery = { next ->
                        getOpenPoolLayoutsPagingQuery(
                            next = next,
                            search = { searchText.ifEmpty { null } },
                            getOpenPoolLayoutsUseCase = getOpenPoolLayoutsUseCase,
                        )
                    },
                )
            },
        )
}
