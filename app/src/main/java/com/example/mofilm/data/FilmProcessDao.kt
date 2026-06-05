package com.example.mofilm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmProcessDao {
    @Query("SELECT * FROM film_processes ORDER BY date DESC")
    fun getAllProcesses(): Flow<List<FilmProcess>>

    @Transaction
    @Query("SELECT * FROM film_processes ORDER BY date DESC")
    fun getProcessesWithScans(): Flow<List<FilmProcessWithScans>>

    @Query("SELECT DISTINCT filmType FROM film_processes ORDER BY filmType ASC")
    fun getUniqueFilmTypes(): Flow<List<String>>

    @Query("SELECT DISTINCT developer FROM film_processes ORDER BY developer ASC")
    fun getUniqueDevelopers(): Flow<List<String>>

    @Query("UPDATE film_processes SET filmType = :newType WHERE filmType = :oldType")
    suspend fun updateFilmType(oldType: String, newType: String)

    @Query("UPDATE film_processes SET developer = :newDev WHERE developer = :oldDev")
    suspend fun updateDeveloper(oldDev: String, newDev: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcess(process: FilmProcess): Long

    @Update
    suspend fun updateProcess(process: FilmProcess)

    @Delete
    suspend fun deleteProcess(process: FilmProcess)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: Scan)

    @Delete
    suspend fun deleteScan(scan: Scan)
    
    @Query("SELECT * FROM scans WHERE processId = :processId")
    fun getScansForProcess(processId: Int): Flow<List<Scan>>
}
