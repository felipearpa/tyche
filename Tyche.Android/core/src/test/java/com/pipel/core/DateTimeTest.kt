package com.pipel.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class DateTimeTest {

    @Test
    fun `given a date with time when its function withoutTime is called then a date without time is returned`() {
        val dateTime = Date()
        val dateWithoutTime = dateTime.withoutTime()
        val calendar = Calendar.getInstance()
        calendar.time = Date(dateWithoutTime.time)
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

}