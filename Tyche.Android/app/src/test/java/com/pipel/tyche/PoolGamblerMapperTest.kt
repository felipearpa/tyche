package com.pipel.tyche

import com.pipel.core.type.Email
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid
import com.pipel.tyche.poolGambler.data.PoolGamblerResponse
import com.pipel.tyche.poolGambler.domain.PoolGambler
import com.pipel.tyche.poolGambler.mapper.PoolGamblerMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PoolGamblerMapperTest {

    @Test
    fun `given a PoolGamblerResponse when is mapped to PoolGambler then a PoolGambler analogous to PoolGamblerResponse is returned`() {
        val poolGamblerResponse = PoolGamblerResponse(
            poolId = Ulid.randomUlid().value,
            gamblerId = Ulid.randomUlid().value,
            gamblerEmail = "email@tyche.com",
            score = 10,
            currentPosition = 1,
            beforePosition = 2
        )
        val poolGambler = PoolGamblerMapper.mapFromDataToDomain(poolGamblerResponse)
        assertEquals(poolGamblerResponse.poolId, poolGambler.poolId.value)
        assertEquals(poolGamblerResponse.gamblerId, poolGambler.gamblerId.value)
        assertEquals(poolGamblerResponse.gamblerEmail, poolGambler.gamblerEmail.value)
        assertEquals(poolGamblerResponse.score, poolGambler.score?.value)
        assertEquals(poolGamblerResponse.currentPosition, poolGambler.currentPosition?.value)
        assertEquals(poolGamblerResponse.beforePosition, poolGambler.beforePosition?.value)
    }

    @Test
    fun `given a PoolGambler when is mapped to PoolGamblerModel then a PoolGamblerModel analogous to PoolGambler is returned`() {
        val poolGambler = PoolGambler(
            poolId = Ulid.randomUlid(),
            gamblerId = Ulid.randomUlid(),
            gamblerEmail = Email("email@tyche.com"),
            score = PositiveInt(10),
            currentPosition = PositiveInt(1),
            beforePosition = PositiveInt(2)
        )
        val poolGamblerModel = PoolGamblerMapper.mapFromDomainToView(poolGambler)
        assertEquals(poolGambler.poolId.value, poolGamblerModel.poolId)
        assertEquals(poolGambler.gamblerId.value, poolGamblerModel.gamblerId)
        assertEquals(poolGambler.gamblerEmail.value, poolGamblerModel.gamblerEmail)
        assertEquals(poolGambler.score?.value, poolGamblerModel.score)
        assertEquals(poolGambler.currentPosition?.value, poolGamblerModel.progress.currentPosition)
        assertEquals(poolGambler.beforePosition?.value, poolGamblerModel.progress.beforePosition)
    }

}