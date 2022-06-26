package com.pipel.tyche.view

data class Progress(
    val currentPosition: Int?,
    val beforePosition: Int?
) {

    fun calculateDifference(): Int? {
        currentPosition?.let { nonNullCurrentPosition ->
            beforePosition?.let { nonNullBeforePosition ->
                return nonNullBeforePosition - nonNullCurrentPosition
            }
            return nonNullCurrentPosition
        }
        return null
    }

}