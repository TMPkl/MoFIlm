package com.example.mofilm.data

import androidx.room.Embedded
import androidx.room.Relation

data class FilmProcessWithScans(
    @Embedded val process: FilmProcess,
    @Relation(
        parentColumn = "id",
        entityColumn = "processId"
    )
    val scans: List<Scan>
)
