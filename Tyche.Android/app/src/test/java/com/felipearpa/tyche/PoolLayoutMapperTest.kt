package com.felipearpa.tyche

import com.felipearpa.core.type.NonEmptyString
import com.felipearpa.core.type.Ulid
import com.felipearpa.tyche.poolLayout.data.PoolLayoutResponse
import com.felipearpa.tyche.poolLayout.domain.PoolLayout
import com.felipearpa.tyche.poolLayout.mapper.PoolLayoutMapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class PoolLayoutMapperTest {

    @Test
    fun `given a PoolLayoutResponse when this one is mapped to PoolLayout then a PoolLayout analogous to PoolLayoutResponse is returned`() {
        val poolLayoutResponse = PoolLayoutResponse(
            poolLayoutId = Ulid.randomUlid().value,
            name = "Copa América 2021",
            startOpeningDateTime = LocalDateTime.now(),
            endOpeningDateTime = LocalDateTime.now()
        )
        val poolLayout = PoolLayoutMapper.map(poolLayoutResponse)
        assertEquals(poolLayoutResponse.poolLayoutId, poolLayout.poolLayoutId.value)
        assertEquals(poolLayoutResponse.name, poolLayout.name.value)
        assertEquals(poolLayoutResponse.startOpeningDateTime, poolLayout.openingStartDateTime)
        assertEquals(poolLayoutResponse.endOpeningDateTime, poolLayout.openingEndDateTime)
    }

    @Test
    fun `given a PoolLayout when this one is mapped to PoolLayoutModel then a PoolLayoutModel analogous to PoolLayout is returned`() {
        val poolLayout = PoolLayout(
            poolLayoutId = Ulid.randomUlid(),
            name = NonEmptyString("Copa América 2021"),
            openingStartDateTime = LocalDateTime.now(),
            openingEndDateTime = LocalDateTime.now()
        )
        val poolLayoutModel = PoolLayoutMapper.map(poolLayout)
        assertEquals(poolLayout.poolLayoutId.value, poolLayoutModel.poolLayoutId)
        assertEquals(poolLayout.name.value, poolLayoutModel.name)
        assertEquals(poolLayout.openingStartDateTime, poolLayoutModel.openingStartDateTime)
        assertEquals(poolLayout.openingEndDateTime, poolLayoutModel.openingEndDateTime)
    }

}