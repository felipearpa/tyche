package com.felipearpa.tyche

import com.felipearpa.core.emptyString
import com.felipearpa.core.paging.emptyCursorPage
import com.quamto.gamblerPool.ui.PoolPagingQuery
import com.quamto.gamblerPool.application.FindPoolsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PoolPagingQueryTest {

    @ExperimentalCoroutinesApi
    @Test
    fun `given a PoolPagingQuery when is performed then FindPoolsLayoutsUseCase is called`() =
        runTest {
            val findPoolsUseCase = mockk<com.quamto.gamblerPool.application.FindPoolsUseCase>()
            coEvery {
                findPoolsUseCase.execute(
                    any(),
                    any(),
                    any()
                )
            } returns emptyCursorPage()

            val poolPagingQuery = com.quamto.gamblerPool.ui.PoolPagingQuery(
                findPoolsUseCase = findPoolsUseCase,
                poolLayoutId = emptyString(),
                filterFunc = { emptyString() })
            poolPagingQuery.execute(emptyString())

            coVerify(exactly = 1) {
                findPoolsUseCase.execute(
                    any(),
                    any(),
                    any()
                )
            }
        }

}