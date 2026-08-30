package com.example.hibreed.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY muscleGroup, name")
    suspend fun getAll(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE name COLLATE NOCASE = :name LIMIT 1")
    suspend fun findByName(name: String): Exercise?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("UPDATE exercises SET name = :name, muscleGroup = :group WHERE id = :id")
    suspend fun update(id: Long, name: String, group: String)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: Long)
}
