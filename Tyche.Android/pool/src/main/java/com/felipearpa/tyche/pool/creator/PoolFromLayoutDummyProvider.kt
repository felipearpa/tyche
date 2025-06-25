package com.felipearpa.tyche.pool.creator

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun poolLayoutFakeModel() =
    PoolLayoutModel(
        id = "X".repeat(15),
        name = "X".repeat(25),
        startDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    )

@OptIn(ExperimentalTime::class)
fun poolLayoutDummyModel() =
    PoolLayoutModel(
        id = "pool123",
        name = "Champions League",
        startDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    )

@OptIn(ExperimentalTime::class)
fun poolLayoutDummyModels() =
    listOf(
        PoolLayoutModel(
            id = "pool123",
            name = "Champions League",
            startDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        ),
        PoolLayoutModel(
            id = "pool456",
            name = "Premier League",
            startDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        ),
        PoolLayoutModel(
            id = "pool789",
            name = "World Cup",
            startDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        ),
    )
