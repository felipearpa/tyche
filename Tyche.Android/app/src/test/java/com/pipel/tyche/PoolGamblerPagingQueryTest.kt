package com.pipel.tyche

import com.pipel.core.CursorPage
import com.pipel.core.empty
import com.pipel.tyche.poolGambler.ui.PoolGamblerPagingQuery
import com.pipel.tyche.poolGambler.useCase.FindPoolsGamblersUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PoolGamblerPagingQueryTest {

    @ExperimentalCoroutinesApi
    @Test
    fun `given a PoolGamblerPagingQuery when is performed then FindPoolsLayoutsUseCase is called`() =
        runTest {
            val findPoolsGamblersUseCase = mockk<FindPoolsGamblersUseCase>()
            coEvery {
                findPoolsGamblersUseCase.execute(
                    any(),
                    any(),
                    any()
                )
            } returns CursorPage.empty()

            val poolGamblerPagingQuery =
                PoolGamblerPagingQuery(
                    findPoolsGamblersUseCase = findPoolsGamblersUseCase,
                    poolId = String.empty(),
                    filterFunc = { String.empty() })
            poolGamblerPagingQuery.execute(String.empty())

            coVerify(exactly = 1) {
                findPoolsGamblersUseCase.execute(
                    any(),
                    any(),
                    any()
                )
            }
        }

}