package com.pipel.tyche.pool.view

data class PoolModel(
    val poolId: String,
    val poolLayoutId: String,
    val poolName: String,
    val currentPosition: Int?,
    val beforePosition: Int?
) {

    fun calculateDifference(): Int? {
        currentPosition?.let { cp ->
            beforePosition?.let { bp ->
                return bp - cp
            }
            return cp
        }
        return null
    }

}