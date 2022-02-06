package com.pipel.tyche

import com.pipel.core.CursorPage
import com.pipel.core.empty
import com.pipel.tyche.poolLayout.ui.PoolLayoutPagingQuery
import com.pipel.tyche.poolLayout.useCase.FindPoolsLayoutsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
                findPoolsLayoutsUseCase.execute(anyString(), anyString())
            ).thenReturn(CursorPage.empty())
            poolLayoutPagingQuery.execute(String.empty())
            verify(findPoolsLayoutsUseCase, Times(1)).execute(anyString(), anyString())
        }

}