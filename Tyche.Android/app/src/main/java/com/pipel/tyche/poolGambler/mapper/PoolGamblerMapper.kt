package com.pipel.tyche.poolGambler.mapper

import com.pipel.core.type.Email
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid
import com.pipel.tyche.poolGambler.data.PoolGamblerResponse
import com.pipel.tyche.poolGambler.domain.PoolGambler
import com.pipel.tyche.poolGambler.view.PoolGamblerModel
import com.pipel.tyche.view.Progress

object PoolGamblerMapper {

    fun mapFromDataToDomain(dataModel: PoolGamblerResponse): PoolGambler =
        PoolGambler(
            poolId = Ulid(dataModel.poolId),
            gamblerId = Ulid(dataModel.gamblerId),
            gamblerEmail = Email(dataModel.gamblerEmail),
            score = dataModel.score?.let { score -> return@let PositiveInt(score) },
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

    fun mapFromDomainToView(domainModel: PoolGambler): PoolGamblerModel =
        PoolGamblerModel(
            poolId = domainModel.poolId.value,
            gamblerId = domainModel.gamblerId.value,
            gamblerEmail = domainModel.gamblerEmail.value,
            score = domainModel.score?.let { score -> return@let score.value },
            progress = Progress(
                domainModel.currentPosition?.value,
                domainModel.beforePosition?.value
            )
        )

}