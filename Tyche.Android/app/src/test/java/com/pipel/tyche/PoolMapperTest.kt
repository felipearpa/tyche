package com.pipel.tyche

import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid
import com.pipel.tyche.pool.data.PoolResponse
import com.pipel.tyche.pool.domain.Pool
import com.pipel.tyche.pool.mapper.PoolMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PoolMapperTest {

    @Test
    fun `given a PoolResponse when is mapped to Pool then a Pool analogous to PoolResponse is returned`() {
        val poolResponse = PoolResponse(
            poolId = Ulid.randomUlid().value,
            poolLayoutId = Ulid.randomUlid().value,
            poolName = "Copa América 2021 Tyche",
            currentPosition = 1,
            beforePosition = 2
        )
        val pool = PoolMapper.mapFromDataToDomain(poolResponse)
        assertEquals(poolResponse.poolId, pool.poolId.value)
        assertEquals(poolResponse.poolLayoutId, pool.poolLayoutId.value)
        assertEquals(poolResponse.poolName, pool.poolName.value)
        assertEquals(poolResponse.currentPosition, pool.currentPosition?.value)
        assertEquals(poolResponse.beforePosition, pool.beforePosition?.value)
    }

    @Test
    fun `given a Pool when this is mapped to PoolModel then a PoolModel analogous to Pool is returned`() {
        val pool = Pool(
            poolId = Ulid.randomUlid(),
            poolLayoutId = Ulid.randomUlid(),
            poolName = NonEmptyString("Copa América 2021 Tyche"),
            currentPosition = PositiveInt(1),
            beforePosition = PositiveInt(2)
        )
        val poolModel = PoolMapper.mapFromDomainToView(pool)
        assertEquals(pool.poolId.value, poolModel.poolId)
        assertEquals(pool.poolLayoutId.value, poolModel.poolLayoutId)
        assertEquals(pool.poolName.value, poolModel.poolName)
        assertEquals(pool.currentPosition?.value, poolModel.progress.currentPosition)
        assertEquals(pool.beforePosition?.value, poolModel.progress.beforePosition)
    }

}