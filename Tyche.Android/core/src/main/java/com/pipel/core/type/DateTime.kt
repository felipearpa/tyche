package com.pipel.core.type

import java.util.*

data class DateTime(val value: Date) {
    override fun toString(): String = value.toString()
}