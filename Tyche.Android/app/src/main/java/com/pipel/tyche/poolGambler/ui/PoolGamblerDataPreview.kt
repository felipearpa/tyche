package com.pipel.tyche.poolGambler.ui

import androidx.paging.PagingData
import com.pipel.core.type.Email
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid
import com.pipel.tyche.poolGambler.domain.PoolGambler
import com.pipel.tyche.poolGambler.mapper.PoolGamblerMapper
import kotlinx.coroutines.flow.flowOf

fun poolsGamblersForPreview() = listOf(
    PoolGambler(
        poolId = Ulid.randomUlid(),
        gamblerId = Ulid.randomUlid(),
        gamblerEmail = Email("fperez@tyche.com"),
        score = PositiveInt(10),
        currentPosition = PositiveInt(1),
        beforePosition = PositiveInt(2)
    ),
    PoolGambler(
        poolId = Ulid.randomUlid(),
        gamblerId = Ulid.randomUlid(),
        gamblerEmail = Email("atabares@tyche.com"),
        score = PositiveInt(1),
        currentPosition = PositiveInt(5),
        beforePosition = PositiveInt(6)
    ),
    PoolGambler(
        poolId = Ulid.randomUlid(),
        gamblerId = Ulid.randomUlid(),
        gamblerEmail = Email("bromero@tyche.com"),
        score = PositiveInt(6),
        currentPosition = PositiveInt(3),
        beforePosition = PositiveInt(3)
    )
)

fun poolsGamblersModelsForPreview() =
    poolsGamblersForPreview().map(PoolGamblerMapper::mapFromDomainToView)

fun poolsGamblersModelsFlowForPreview() = flowOf(PagingData.from(poolsGamblersModelsForPreview()))