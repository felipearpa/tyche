package com.pipel.tyche

import com.pipel.core.Page
import com.pipel.core.empty
import com.pipel.tyche.ui.poollayout.PoolLayoutPagingQuery
import com.pipel.tyche.usecase.FindPoolsLayoutsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.internal.verification.Times

class PoolLayoutPagingQueryTest {

    private lateinit var poolLayoutPagingQuery: PoolLayoutPagingQuery

    @Mock
    private lateinit var findPoolsLayoutsUseCase: FindPoolsLayoutsUseCase

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        poolLayoutPagingQuery = PoolLayoutPagingQuery(
            findPoolsLayoutsUseCase = findPoolsLayoutsUseCase,
            filterFunc = { String.empty() })
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `given a FindPoolsLayoutsUseCase when the function execute of PoolLayoutPagingQuery is executed then the function execute of FindPoolsLayoutsUseCase is called`() =
        runBlockingTest {
            `when`(
                findPoolsLayoutsUseCase.execute(
                    anyInt(),
                    anyInt(),
                    anyString()
                )
            ).thenReturn(Page.empty())
            poolLayoutPagingQuery.execute(0, 0)
            verify(findPoolsLayoutsUseCase, Times(1)).execute(anyInt(), anyInt(), anyString())
        }

}