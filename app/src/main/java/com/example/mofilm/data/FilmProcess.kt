package com.example.mofilm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "film_processes")
data class FilmProcess(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val filmType: String,
    val isPushPull: Boolean,
    val pushPullValue: String?,
    val developer: String,
    val temperature: Double,
    val developingTime: String,
    val dilution: String,
    val processDescription: String,
    val comments: String
)
