package com.example.hibreed.ui

import com.example.hibreed.data.WorkoutWithEverything

/** A single flattened list item that is either a day header or one workout row. */
sealed class SessionListItem {
    data class DayHeader(val dateKey: String, val displayDate: String) : SessionListItem()
    data class Workout(val workout: WorkoutWithEverything) : SessionListItem()
}
