package com.example.hibreed.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction

data class WorkoutWithEverything(
    val id: Long,
    val exerciseId: Long,
    val date: String,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val sets: List<Set>
)

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts ORDER BY date DESC, id DESC")
    suspend fun getAllWithExercise(): List<WorkoutWithEverything>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithSets(workoutId: Long): WorkoutWithEverything?

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Insert
    suspend fun insertSet(set: Set): Long

    @Query("DELETE FROM sets WHERE workoutId = :workoutId")
    suspend fun deleteSetsForWorkout(workoutId: Long)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    // Full replace of a workout's sets (used when editing / saving).
    @Transaction
    suspend fun replaceSets(workoutId: Long, sets: List<Set>) {
        deleteSetsForWorkout(workoutId)
        sets.forEachIndexed { index, s ->
            insertSet(
                Set(
                    workoutId = workoutId,
                    weightLbs = s.weightLbs,
                    reps = s.reps,
                    sortOrder = index
                )
            )
        }
    }
}
