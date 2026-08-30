package com.example.hibreed

import android.app.Application
import com.example.hibreed.data.AppDatabase
import com.example.hibreed.data.ExerciseDao
import com.example.hibreed.data.WorkoutDao

class HibreedApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val exerciseDao: ExerciseDao by lazy { database.exerciseDao() }
    val workoutDao: WorkoutDao by lazy { database.workoutDao() }
}
