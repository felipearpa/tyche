package com.felipearpa.network.serializer

import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LocalDateTimeSerializerTest {

    @Serializable
    data class SampleData(
        @Serializable(with = LocalDateTimeSerializer::class)
        val timestamp: LocalDateTime,
    )

    @Test
    fun `should serialize LocalDateTime to ISO-8601 string`() {
        val dateTime = LocalDateTime.of(2023, 6, 1, 14, 30, 0)
        val json = Json.encodeToString(SampleData(dateTime))
        json shouldBeEqual """{"timestamp":"2023-06-01T14:30:00"}"""
    }

    @Test
    fun `should deserialize ISO-8601 string to LocalDateTime`() {
        val json = """{"timestamp":"2023-06-01T14:30:00"}"""
        val expected = LocalDateTime.of(2023, 6, 1, 14, 30, 0)
        val data = Json.decodeFromString<SampleData>(json)
        data.timestamp shouldBeEqual expected
    }
}
