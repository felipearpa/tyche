package com.pipel.tyche.pool.mapper

import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid
import com.pipel.tyche.pool.data.PoolResponse
import com.pipel.tyche.pool.domain.Pool
import com.pipel.tyche.pool.view.PoolModel

object PoolMapper {

    fun mapFromDataToDomain(dataModel: PoolResponse): Pool =
        Pool(
            poolId = Ulid(dataModel.poolId),
            poolLayoutId = Ulid(dataModel.poolLayoutId),
            poolName = NonEmptyString(dataModel.poolName),
            currentPosition = dataModel.currentPosition?.let { currentPosition ->
                return@let PositiveInt(
                    currentPosition
                )
            },
            beforePosition = dataModel.beforePosition?.let { beforePosition ->
                return@let PositiveInt(
                    beforePosition
                )
            }
        )

    fun mapFromDomainToView(domainModel: Pool): PoolModel =
        PoolModel(
            poolId = domainModel.poolId.value,
            poolLayoutId = domainModel.poolLayoutId.value,
            poolName = domainModel.poolName.value,
            currentPosition = domainModel.currentPosition?.let { currentPosition -> return@let currentPosition.value },
            beforePosition = domainModel.beforePosition?.let { beforePosition -> return@let beforePosition.value }
        )

}