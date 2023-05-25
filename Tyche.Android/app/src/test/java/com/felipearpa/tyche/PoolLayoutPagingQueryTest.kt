package com.felipearpa.tyche

import com.felipearpa.core.emptyString
import com.felipearpa.core.paging.emptyCursorPage
import com.felipearpa.tyche.poolLayout.ui.PoolLayoutPagingQuery
import com.felipearpa.tyche.poolLayout.domain.FindActivePoolsLayoutsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PoolLayoutPagingQueryTest {

    @ExperimentalCoroutinesApi
    @Test
    fun `given a FindPoolsLayoutsUseCase when the function execute of PoolLayoutPagingQuery is executed then the function execute of FindPoolsLayoutsUseCase is called`() =
        runTest {
            val findActivePoolsLayoutsUseCase = mockk<FindActivePoolsLayoutsUseCase>()
            coEvery {
                findActivePoolsLayoutsUseCase.execute(
                    any(),
                    any()
                )
            } returns emptyCursorPage()

            val poolLayoutPagingQuery =
                PoolLayoutPagingQuery(findActivePoolsLayoutsUseCase = findActivePoolsLayoutsUseCase)
            poolLayoutPagingQuery.execute(emptyString())

            coVerify(exactly = 1) {
                findActivePoolsLayoutsUseCase.execute(
                    any(),
                    any()
                )
            }
        }

}