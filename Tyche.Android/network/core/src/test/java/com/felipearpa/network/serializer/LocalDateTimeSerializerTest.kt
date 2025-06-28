package com.felipearpa.network.serializer

import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class LocalDateTimeSerializerTest {

    @Serializable
    data class SampleData(
        @Serializable(with = LocalDateTimeSerializer::class)
        val timestamp: LocalDateTime,
    )

    @Test
    fun `should serialize LocalDateTime to ISO-8601 string`() {
        val dateTime =
            LocalDateTime(year = 2023, month = 6, day = 1, hour = 14, minute = 30, second = 0)
        val json = Json.encodeToString(SampleData(dateTime))
        json shouldBeEqual """{"timestamp":"2023-06-01T14:30:00-05:00"}"""
    }

    @Test
    fun `should deserialize ISO-8601 string to LocalDateTime`() {
        val json = """{"timestamp":"2023-06-01T14:30:00"}"""
        val expected = LocalDateTime(2023, 6, 1, 14, 30, 0)
        val data = Json.decodeFromString<SampleData>(json)
        data.timestamp shouldBeEqual expected
    }
}
