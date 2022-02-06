package com.pipel.tyche

import com.pipel.core.type.DateTime
import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.Ulid
import com.pipel.tyche.poolLayout.data.PoolLayoutResponse
import com.pipel.tyche.poolLayout.domain.PoolLayout
import com.pipel.tyche.poolLayout.mapper.PoolLayoutMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class PoolLayoutMapperTest {

    @Test
    fun `given a PoolLayoutResponse when this one is mapped to PoolLayout then a PoolLayout analogous to PoolLayoutResponse is returned`() {
        val poolLayoutResponse = PoolLayoutResponse(
            poolLayoutId = Ulid.randomUlid().value,
            name = "Copa América 2021",
            startOpeningDateTime = Date(),
            endOpeningDateTime = Date()
        )
        val poolLayout = PoolLayoutMapper.mapFromDataToDomain(poolLayoutResponse)
        assertEquals(poolLayoutResponse.poolLayoutId, poolLayout.poolLayoutId.value)
        assertEquals(poolLayoutResponse.name, poolLayout.name.value)
        assertEquals(poolLayoutResponse.startOpeningDateTime, poolLayout.openingStartDateTime.value)
        assertEquals(poolLayoutResponse.endOpeningDateTime, poolLayout.openingEndDateTime.value)
    }

    @Test
    fun `given a PoolLayout when this one is mapped to PoolLayoutModel then a PoolLayoutModel analogous to PoolLayout is returned`() {
        val poolLayout = PoolLayout(
            poolLayoutId = Ulid.randomUlid(),
            name = NonEmptyString("Copa América 2021"),
            openingStartDateTime = DateTime(Date()),
            openingEndDateTime = DateTime(Date())
        )
        val poolLayoutModel = PoolLayoutMapper.mapFromDomainToView(poolLayout)
        assertEquals(poolLayout.poolLayoutId.value, poolLayoutModel.poolLayoutId)
        assertEquals(poolLayout.name.value, poolLayoutModel.name)
        assertEquals(poolLayout.openingStartDateTime.value, poolLayoutModel.openingStartDateTime)
        assertEquals(poolLayout.openingEndDateTime.value, poolLayoutModel.openingEndDateTime)
    }

}