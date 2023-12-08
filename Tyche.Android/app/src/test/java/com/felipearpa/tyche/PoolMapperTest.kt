package com.felipearpa.tyche

import com.felipearpa.core.type.NonEmptyString
import com.felipearpa.core.type.Ulid
import com.quamto.gamblerPool.domain.PoolResponse
import com.felipearpa.tyche.poolhome.mapper.PoolMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class PoolMapperTest {

    @Test
    fun `given a PoolResponse when is mapped to Pool then a Pool analogous to PoolResponse is returned`() {
        val poolResponse = com.quamto.gamblerPool.domain.PoolResponse(
            poolId = Ulid.randomUlid().value,
            poolLayoutId = Ulid.randomUlid().value,
            poolName = "Copa América 2021 Tyche",
            currentPosition = 1,
            beforePosition = 2
        )
        val pool = PoolMapper.map(poolResponse)
        assertEquals(poolResponse.poolId, pool.poolId.value)
        assertEquals(poolResponse.poolLayoutId, pool.poolLayoutId.value)
        assertEquals(poolResponse.poolName, pool.poolName.value)
        assertEquals(poolResponse.currentPosition, pool.currentPosition)
        assertEquals(poolResponse.beforePosition, pool.beforePosition)
    }

    @Test
    fun `given a Pool when this is mapped to PoolModel then a PoolModel analogous to Pool is returned`() {
        val pool = Pool(
            poolId = Ulid.randomUlid(),
            poolLayoutId = Ulid.randomUlid(),
            poolName = NonEmptyString("Copa América 2021 Tyche"),
            currentPosition = 1,
            beforePosition = 2
        )
        val poolModel = PoolMapper.map(pool)
        assertEquals(pool.poolId.value, poolModel.poolId)
        assertEquals(pool.poolLayoutId.value, poolModel.poolLayoutId)
        assertEquals(pool.poolName.value, poolModel.poolName)
        assertEquals(pool.currentPosition, poolModel.currentPosition)
        assertEquals(pool.beforePosition, poolModel.beforePosition)
    }

}