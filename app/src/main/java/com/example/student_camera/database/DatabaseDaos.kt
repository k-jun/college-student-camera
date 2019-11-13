package com.example.student_camera.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PhotoDatabaseDao {
    @Insert
    fun insert(photo: Photo)

    @Update
    fun update(photo: Photo)

    @Query("SELECT * FROM photo WHERE photoId = :key")
    fun get(key: Long): Photo?

    @Query("DELETE FROM photo")
    fun clear()

    @Query("SELECT * FROM photo ORDER BY photoId DESC LIMIT 1")
    fun getAllLast(): Photo?

    @Query("SELECT * FROM photo WHERE day_cell_num = :dayCell ORDER BY photoId DESC LIMIT 5")
    fun getSelectedLast(dayCell: Int): List<Photo>

    @Query("SELECT * FROM photo WHERE day_cell_num = :dayCell AND time_cell_num = :timeCell ORDER BY photoId DESC LIMIT 5")
    fun getSelectedCell(dayCell: Int, timeCell: Int): List<Photo>

    @Query("SELECT * FROM photo")
    fun getAll(): List<Photo>
}

@Dao
interface TimeScheduleDatabaseDao {
    @Insert
    fun insert(ts: TimeSchedule)

    @Update
    fun update(ts: TimeSchedule)

    @Query("SELECT * from time_schedule WHERE num = :num")
    fun get(num: Int): TimeSchedule?

    @Query("SELECT * FROM time_schedule ORDER BY num")
    fun getAll(): List<TimeSchedule>
}