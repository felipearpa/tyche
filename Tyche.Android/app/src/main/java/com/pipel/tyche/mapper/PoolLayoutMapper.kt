package com.pipel.tyche.mapper

import com.pipel.core.type.DateTime
import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.Uuid
import com.pipel.tyche.data.PoolLayoutResponse
import com.pipel.tyche.domain.PoolLayout
import com.pipel.tyche.ui.poollayout.PoolLayoutModel

object PoolLayoutMapper {

    fun mapFromDataToDomain(dataModel: PoolLayoutResponse): PoolLayout {
        return PoolLayout(
            Uuid(dataModel.poolLayoutId),
            NonEmptyString(dataModel.name),
            DateTime(dataModel.openingStartDateTime),
            DateTime(dataModel.openingEndDateTime)
        )
    }

    fun mapFromDomainToView(domainModel: PoolLayout): PoolLayoutModel {
        return PoolLayoutModel(
            domainModel.poolLayoutId.value,
            domainModel.name.value,
            domainModel.openingStartDateTime.value,
            domainModel.openingEndDateTime.value
        )
    }

}