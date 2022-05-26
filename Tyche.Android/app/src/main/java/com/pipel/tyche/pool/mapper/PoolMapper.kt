package com.pipel.tyche.pool.mapper

import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid
import com.pipel.tyche.pool.data.PoolResponse
import com.pipel.tyche.pool.domain.Pool
import com.pipel.tyche.pool.view.PoolModel

object PoolMapper {

    fun mapFromDataToDomain(dataModel: PoolResponse): Pool {
        return Pool(
            poolId = Ulid(dataModel.poolId),
            poolLayoutId = Ulid(dataModel.poolLayoutId),
            poolName = NonEmptyString(dataModel.poolName),
            currentPosition = if (dataModel.currentPosition != null) PositiveInt(dataModel.currentPosition) else null,
            beforePosition = if (dataModel.beforePosition != null) PositiveInt(dataModel.beforePosition) else null
        )
    }

    fun mapFromDomainToView(domainModel: Pool): PoolModel {
        return PoolModel(
            poolId = domainModel.poolId.value,
            poolLayoutId = domainModel.poolLayoutId.value,
            poolName = domainModel.poolName.value,
            currentPosition = if (domainModel.currentPosition != null) domainModel.currentPosition.value else null,
            beforePosition = if (domainModel.beforePosition != null) domainModel.beforePosition.value else null
        )
    }

}