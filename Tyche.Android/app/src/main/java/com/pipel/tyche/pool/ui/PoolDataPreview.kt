package com.pipel.tyche.pool.ui

import androidx.paging.PagingData
import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid
import com.pipel.tyche.pool.domain.Pool
import com.pipel.tyche.pool.mapper.PoolMapper
import com.pipel.tyche.pool.view.PoolModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

fun poolsForPreview(): List<Pool> {
    return listOf(
        Pool(
            poolId = Ulid.randomUlid(),
            poolLayoutId = Ulid.randomUlid(),
            poolName = NonEmptyString("Copa América 2022 UTP"),
            currentPosition = PositiveInt(1),
            beforePosition = null
        )
    )
}

fun poolsModelsForPreview(): List<PoolModel> =
    poolsForPreview().map(PoolMapper::mapFromDomainToView)

fun poolsModelsFlowForPreview(): Flow<PagingData<PoolModel>> =
    flowOf(PagingData.from(poolsModelsForPreview()))