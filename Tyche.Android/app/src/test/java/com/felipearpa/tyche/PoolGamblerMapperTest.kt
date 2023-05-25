package com.felipearpa.tyche

import com.felipearpa.core.type.Email
import com.felipearpa.core.type.Ulid
import com.felipearpa.tyche.poolGambler.data.PoolGamblerResponse
import com.felipearpa.tyche.poolGambler.domain.PoolGambler
import com.felipearpa.tyche.poolGambler.mapper.PoolGamblerMapper
import org.junit.Assert.assertEquals
import org.junit.Test

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
        val poolGambler = PoolGamblerMapper.map(poolGamblerResponse)
        assertEquals(poolGamblerResponse.poolId, poolGambler.poolId.value)
        assertEquals(poolGamblerResponse.gamblerId, poolGambler.gamblerId.value)
        assertEquals(poolGamblerResponse.gamblerEmail, poolGambler.gamblerEmail.value)
        assertEquals(poolGamblerResponse.score, poolGambler.score)
        assertEquals(poolGamblerResponse.currentPosition, poolGambler.currentPosition)
        assertEquals(poolGamblerResponse.beforePosition, poolGambler.beforePosition)
    }

    @Test
    fun `given a PoolGambler when is mapped to PoolGamblerModel then a PoolGamblerModel analogous to PoolGambler is returned`() {
        val poolGambler = PoolGambler(
            poolId = Ulid.randomUlid(),
            gamblerId = Ulid.randomUlid(),
            gamblerEmail = Email("email@tyche.com"),
            score = 10,
            currentPosition = 1,
            beforePosition = 2
        )
        val poolGamblerModel = PoolGamblerMapper.map(poolGambler)
        assertEquals(poolGambler.poolId.value, poolGamblerModel.poolId)
        assertEquals(poolGambler.gamblerId.value, poolGamblerModel.gamblerId)
        assertEquals(poolGambler.gamblerEmail.value, poolGamblerModel.gamblerEmail)
        assertEquals(poolGambler.score, poolGamblerModel.score)
        assertEquals(poolGambler.currentPosition, poolGamblerModel.progress.currentPosition)
        assertEquals(poolGambler.beforePosition, poolGamblerModel.progress.beforePosition)
    }

}