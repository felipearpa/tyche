package com.felipearpa.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun LocalDateTime.toLocalDateString(): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    return formatter.format(this)
}

fun LocalDateTime.toLocalDateTimeString(): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    return formatter.format(this)
}

fun LocalDateTime.toLocalTimeString(): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return formatter.format(this)
}