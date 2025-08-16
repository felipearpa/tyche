package com.felipearpa.tyche.ui.exception

import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.ui.network.NetworkLocalizedException
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ThrowableTransformerTest {
    @Test
    fun `given a LocalizedException when transformation attempt then the same LocalizedException is returned`() {
        val newLocalizedException = localizedException.orDefaultLocalized()
        newLocalizedException shouldBeSameInstanceAs localizedException
    }

    @TestFactory
    fun `given a NetworkException when transformation attempt then is transformed to NetworkLocalizedException`() =
        listOf(
            NetworkException.RemoteCommunication,
            NetworkException.Http(HttpStatusCode.INTERNAL_SERVER_ERROR)
        ).map { networkException ->
            dynamicTest("given a $networkException when transformation attempt then is transformed to NetworkLocalizedException") {
                val newLocalizedException = networkException.orDefaultLocalized()
                newLocalizedException should beInstanceOf<NetworkLocalizedException>()
            }
        }

    @Test
    fun `given a neither LocalizedException nor NetworkException when transformation attempt then is transformed to UnknownLocalizedException`() {
        val newLocalizedException = neitherLocalizedNorNetworkException.orDefaultLocalized()
        newLocalizedException should beInstanceOf<UnknownLocalizedException>()
    }

    @Test
    fun `given a LocalizedException when checked for LocalizedException type then the same LocalizedException is returned`() {
        val newLocalizedException = localizedException.localizedOrNull()
        newLocalizedException shouldBeSameInstanceAs localizedException
    }

    @Test
    fun `given a no LocalizedException when checked for LocalizedException type then null is returned`() {
        val newLocalizedException = noLocalizedException.localizedOrNull()
        newLocalizedException.shouldBeNull()
    }
}

private val localizedException = LocalizedException()
private val neitherLocalizedNorNetworkException = RuntimeException()
private val noLocalizedException = RuntimeException()