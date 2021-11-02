package com.pipel.tyche.ui.poollayout

import androidx.paging.PagingData
import com.pipel.core.type.DateTime
import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.Uuid
import com.pipel.tyche.domain.PoolLayout
import com.pipel.tyche.mapper.PoolLayoutMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.*

fun poolsLayoutsForPreview(): List<PoolLayout> {
    return listOf(
        PoolLayout(
            poolLayoutId = Uuid.randomUUID(),
            name = NonEmptyString("FIFA World Cup Qatar 2022"),
            openingStartDateTime = DateTime(Date()),
            openingEndDateTime = DateTime(Date())
        ),
        PoolLayout(
            poolLayoutId = Uuid.randomUUID(),
            name = NonEmptyString("American Cup 2021"),
            openingStartDateTime = DateTime(Date()),
            openingEndDateTime = DateTime(Date())
        ),
        PoolLayout(
            poolLayoutId = Uuid.randomUUID(),
            name = NonEmptyString("Previous round of American at October to Qatar 2022"),
            openingStartDateTime = DateTime(Date()),
            openingEndDateTime = DateTime(Date())
        )
    )
}

fun poolsLayoutsModelsForPreview(): List<PoolLayoutModel> =
    poolsLayoutsForPreview().map(PoolLayoutMapper::mapFromDomainToView)

fun poolLayoutFlowForPreview(): Flow<PagingData<PoolLayoutModel>> =
    flowOf(PagingData.from(poolsLayoutsModelsForPreview()))