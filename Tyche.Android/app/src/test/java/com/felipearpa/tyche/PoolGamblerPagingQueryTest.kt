package com.felipearpa.tyche

import com.felipearpa.core.emptyString
import com.felipearpa.core.paging.emptyCursorPage
import com.felipearpa.tyche.poolGambler.ui.PoolGamblerPagingQuery
import com.felipearpa.tyche.poolGambler.domain.FindPoolsGamblersUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

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
            } returns emptyCursorPage()

            val poolGamblerPagingQuery =
                PoolGamblerPagingQuery(
                    findPoolsGamblersUseCase = findPoolsGamblersUseCase,
                    poolId = emptyString(),
                    filterFunc = { emptyString() })
            poolGamblerPagingQuery.execute(emptyString())

            coVerify(exactly = 1) {
                findPoolsGamblersUseCase.execute(
                    any(),
                    any(),
                    any()
                )
            }
        }

}