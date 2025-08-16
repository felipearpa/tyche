package com.felipearpa.network.serializer

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.format.DateTimeFormatter

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val javaLocalDateTime = value.toJavaLocalDateTime()
        val offsetDateTime =
            javaLocalDateTime.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime()
        encoder.encodeString(offsetDateTime.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return java.time.LocalDateTime.parse(decoder.decodeString(), formatter)
            .toKotlinLocalDateTime()
    }
}
