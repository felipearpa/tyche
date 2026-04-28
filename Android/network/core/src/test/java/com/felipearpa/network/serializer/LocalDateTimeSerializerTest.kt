package com.felipearpa.network.serializer

import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.TimeZone

class LocalDateTimeSerializerTest {

    @Serializable
    data class SampleData(
        @Serializable(with = LocalDateTimeSerializer::class)
        val timestamp: LocalDateTime,
    )

    private lateinit var originalTimeZone: TimeZone

    @BeforeEach
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"))
    }

    @AfterEach
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `should serialize local LocalDateTime to UTC ISO-8601 string`() {
        val dateTime =
            LocalDateTime(year = 2023, month = 6, day = 1, hour = 14, minute = 30, second = 0)
        val json = Json.encodeToString(SampleData(dateTime))
        json shouldBeEqual """{"timestamp":"2023-06-01T19:30:00Z"}"""
    }

    @Test
    fun `should deserialize UTC ISO-8601 string to local LocalDateTime`() {
        val json = """{"timestamp":"2023-06-01T18:30:00Z"}"""
        val expected = LocalDateTime(2023, 6, 1, 13, 30, 0)
        val data = Json.decodeFromString<SampleData>(json)
        data.timestamp shouldBeEqual expected
    }

    @Test
    fun `should deserialize offset ISO-8601 string to local LocalDateTime`() {
        val json = """{"timestamp":"2023-06-01T18:30:00+00:00"}"""
        val expected = LocalDateTime(2023, 6, 1, 13, 30, 0)
        val data = Json.decodeFromString<SampleData>(json)
        data.timestamp shouldBeEqual expected
    }
}
