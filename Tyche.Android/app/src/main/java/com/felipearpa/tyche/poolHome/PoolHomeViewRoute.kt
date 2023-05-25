package com.felipearpa.tyche.poolHome

object PoolHomeViewRoute {

    enum class Params(val identifier: String) {
        POOL_ID("poolId"),
        GAMBLER_ID("gamblerId")
    }

    val route: String =
        "pool/{${Params.POOL_ID.identifier}}/gambler/{${Params.GAMBLER_ID.identifier}}/home"

    fun route(poolId: String, gamblerId: String) =
        route
            .replace("{${Params.POOL_ID.identifier}}", poolId)
            .replace("{${Params.GAMBLER_ID.identifier}}", gamblerId)
}