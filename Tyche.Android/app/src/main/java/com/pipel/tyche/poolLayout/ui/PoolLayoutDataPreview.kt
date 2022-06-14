package com.pipel.tyche.poolLayout.ui

import androidx.paging.PagingData
import com.pipel.core.type.DateTime
import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.Ulid
import com.pipel.tyche.poolLayout.domain.PoolLayout
import com.pipel.tyche.poolLayout.mapper.PoolLayoutMapper
import com.pipel.tyche.poolLayout.view.PoolLayoutModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.*

fun poolsLayoutsForPreview(): List<PoolLayout> = listOf(
    PoolLayout(
        poolLayoutId = Ulid.randomUlid(),
        name = NonEmptyString("FIFA World Cup Qatar 2022"),
        openingStartDateTime = DateTime(Date()),
        openingEndDateTime = DateTime(Date())
    ),
    PoolLayout(
        poolLayoutId = Ulid.randomUlid(),
        name = NonEmptyString("American Cup 2021"),
        openingStartDateTime = DateTime(Date()),
        openingEndDateTime = DateTime(Date())
    ),
    PoolLayout(
        poolLayoutId = Ulid.randomUlid(),
        name = NonEmptyString("Previous round of American at October to Qatar 2022"),
        openingStartDateTime = DateTime(Date()),
        openingEndDateTime = DateTime(Date())
    )
)

fun poolsLayoutsModelsForPreview(): List<PoolLayoutModel> =
    poolsLayoutsForPreview().map(PoolLayoutMapper::mapFromDomainToView)

fun poolsLayoutsModelsFlowForPreview(): Flow<PagingData<PoolLayoutModel>> =
    flowOf(PagingData.from(poolsLayoutsModelsForPreview()))