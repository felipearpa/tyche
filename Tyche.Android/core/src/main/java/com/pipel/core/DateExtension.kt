package com.pipel.core

import java.text.DateFormat
import java.util.*

fun Date.withoutTime(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = Date(this.time)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

fun Date.addDays(days: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = Date(this.time)
    calendar.add(Calendar.DATE, days)
    return calendar.time
}

fun Date.toLocalDateString(): String {
    val formatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    return formatter.format(this)
}

fun Date.toLocalDateTimeString(): String {
    val formatter =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
    return formatter.format(this)
}