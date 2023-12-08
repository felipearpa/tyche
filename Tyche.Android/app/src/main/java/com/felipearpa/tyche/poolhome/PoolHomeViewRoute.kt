package com.felipearpa.tyche.poolhome

object PoolHomeViewRoute {
    enum class Param(val id: String) {
        POOL_ID("poolId"),
        GAMBLER_ID("gamblerId")
    }

    val route: String =
        "pool/{${Param.POOL_ID.id}}/gambler/{${Param.GAMBLER_ID.id}}/home"

    fun route(poolId: String, gamblerId: String) =
        route
            .replace("{${Param.POOL_ID.id}}", poolId)
            .replace("{${Param.GAMBLER_ID.id}}", gamblerId)
}