package com.felipearpa.tyche.pool.poolscore

object PoolScoreListRoute {

    enum class Param(val id: String) {
        GAMBLER_ID("gamblerId")
    }

    val route: String = "gambler/{${Param.GAMBLER_ID.id}}/pools"

    fun route(gamblerId: String) = route.replace("{${Param.GAMBLER_ID.id}}", gamblerId)
}