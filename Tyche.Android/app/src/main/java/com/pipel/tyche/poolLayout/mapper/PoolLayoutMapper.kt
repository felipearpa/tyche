package com.pipel.tyche.poolLayout.mapper

import com.pipel.core.type.DateTime
import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.Ulid
import com.pipel.tyche.poolLayout.data.PoolLayoutResponse
import com.pipel.tyche.poolLayout.domain.PoolLayout
import com.pipel.tyche.poolLayout.view.PoolLayoutModel

object PoolLayoutMapper {

    fun mapFromDataToDomain(dataModel: PoolLayoutResponse): PoolLayout =
        PoolLayout(
            Ulid(dataModel.poolLayoutId),
            NonEmptyString(dataModel.name),
            DateTime(dataModel.startOpeningDateTime),
            DateTime(dataModel.endOpeningDateTime)
        )

    fun mapFromDomainToView(domainModel: PoolLayout): PoolLayoutModel =
        PoolLayoutModel(
            domainModel.poolLayoutId.value,
            domainModel.name.value,
            domainModel.openingStartDateTime.value,
            domainModel.openingEndDateTime.value
        )

}