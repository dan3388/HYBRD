package com.example.hibreed.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps access to exercises and the built-in library.
 * On first run it seeds the database with common exercises grouped by muscle group.
 */
class ExerciseRepository(
    private val appContext: Context,
    private val dao: ExerciseDao
) {

    companion object {
        private const val PREFS = "hibreed_prefs"
        private const val KEY_LIBRARY_SEEDED = "library_seeded_v2"
    }

    suspend fun getAll(): List<Exercise> = dao.getAll()

    suspend fun addOrGet(name: String, muscleGroup: String): Exercise? {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return null

        dao.findByName(cleanName)?.let { return it }

        val id = dao.insert(
            Exercise(
                name = cleanName,
                muscleGroup = muscleGroup,
                isCustom = true
            )
        )
        return if (id > 0) {
            Exercise(id = id, name = cleanName, muscleGroup = muscleGroup, isCustom = true)
        } else {
            dao.findByName(cleanName)
        }
    }

    suspend fun update(id: Long, name: String, muscleGroup: String) {
        dao.update(id, name.trim(), muscleGroup)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }

    /** Seeds the built-in library once. Safe to call on every launch. */
    suspend fun seedLibraryIfNeeded() {
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_LIBRARY_SEEDED, false)) return

        withContext(Dispatchers.IO) {
            dao.insertAll(buildLibrary())
            prefs.edit().putBoolean(KEY_LIBRARY_SEEDED, true).apply()
        }
    }

    private fun buildLibrary(): List<Exercise> {
        val list = mutableListOf<Exercise>()

        fun add(group: String, vararg names: String) {
            names.forEach { list.add(Exercise(name = it, muscleGroup = group, isCustom = false)) }
        }

        add("Chest", "Bench Press", "Incline Bench Press", "Decline Bench Press", "Dumbbell Bench Press", "Dumbbell Flyes", "Cable Crossover", "Push-ups", "Chest Dip", "Machine Chest Press")
        add("Back", "Deadlift", "Barbell Row", "Pull-ups", "Chin-ups", "Lat Pulldown", "Seated Cable Row", "T-Bar Row", "Single-Arm Dumbbell Row", "Face Pull", "Straight-Arm Pulldown", "Back Extension")
        add("Shoulders", "Overhead Press", "Seated Dumbbell Press", "Arnold Press", "Lateral Raise", "Front Raise", "Rear Delt Fly", "Upright Row", "Shrugs", "Cable Lateral Raise")
        add("Biceps", "Barbell Curl", "Dumbbell Curl", "Hammer Curl", "Preacher Curl", "Incline Dumbbell Curl", "Concentration Curl", "Cable Curl", "EZ-Bar Curl")
        add("Triceps", "Tricep Pushdown", "Skullcrushers", "Close-Grip Bench Press", "Overhead Tricep Extension", "Dips", "Rope Pushdown", "Diamond Push-ups")
        add("Legs", "Squat", "Front Squat", "Leg Press", "Leg Extension", "Lying Leg Curl", "Seated Leg Curl", "Romanian Deadlift", "Lunges", "Bulgarian Split Squat", "Standing Calf Raise", "Seated Calf Raise", "Hip Thrust", "Glute Bridge", "Step-ups", "Sumo Deadlift")
        add("Core", "Plank", "Crunches", "Russian Twist", "Leg Raises", "Hanging Leg Raise", "Mountain Climbers", "Ab Wheel Rollout", "Bicycle Crunch", "Side Plank")
        add("Full Body", "Clean and Jerk", "Snatch", "Thruster", "Burpees", "Kettlebell Swing", "Farmers Walk", "Sled Push", "Man Maker")

        return list
    }
}
