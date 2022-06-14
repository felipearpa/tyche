package com.pipel.tyche

import com.pipel.core.CursorPage
import com.pipel.core.empty
import com.pipel.tyche.poolLayout.ui.PoolLayoutPagingQuery
import com.pipel.tyche.poolLayout.useCase.FindActivePoolsLayoutsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

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
            } returns CursorPage.empty()

            val poolLayoutPagingQuery =
                PoolLayoutPagingQuery(findActivePoolsLayoutsUseCase = findActivePoolsLayoutsUseCase)
            poolLayoutPagingQuery.execute(String.empty())

            coVerify(exactly = 1) {
                findActivePoolsLayoutsUseCase.execute(
                    any(),
                    any()
                )
            }
        }

}