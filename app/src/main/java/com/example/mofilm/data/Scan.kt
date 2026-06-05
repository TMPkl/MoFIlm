package com.example.mofilm.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scans",
    foreignKeys = [
        ForeignKey(
            entity = FilmProcess::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["processId"])]
)
data class Scan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uri: String,
    val processId: Int
)
