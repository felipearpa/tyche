package com.felipearpa.pool.ui.poolScore

object PoolScoreListRoute {

    enum class Params(val identifier: String) {
        GAMBLER_ID("gamblerId")
    }

    val route: String = "gambler/{${Params.GAMBLER_ID.identifier}}/pools"

    fun route(gamblerId: String) = route.replace("{${Params.GAMBLER_ID.identifier}}", gamblerId)

}