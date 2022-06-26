package com.pipel.tyche

import com.pipel.core.CursorPage
import com.pipel.core.empty
import com.pipel.tyche.pool.ui.PoolPagingQuery
import com.pipel.tyche.pool.useCase.FindPoolsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PoolPagingQueryTest {

    @ExperimentalCoroutinesApi
    @Test
    fun `given a PoolPagingQuery when is performed then FindPoolsLayoutsUseCase is called`() =
        runTest {
            val findPoolsUseCase = mockk<FindPoolsUseCase>()
            coEvery {
                findPoolsUseCase.execute(
                    any(),
                    any(),
                    any()
                )
            } returns CursorPage.empty()

            val poolPagingQuery = PoolPagingQuery(
                findPoolsUseCase = findPoolsUseCase,
                poolLayoutId = String.empty(),
                filterFunc = { String.empty() })
            poolPagingQuery.execute(String.empty())

            coVerify(exactly = 1) {
                findPoolsUseCase.execute(
                    any(),
                    any(),
                    any()
                )
            }
        }

}