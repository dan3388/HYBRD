package com.example.hibreed.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class Set(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val weightLbs: Double,
    val reps: Int,
    val sortOrder: Int
)
