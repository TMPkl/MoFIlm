package com.example.mofilm.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mofilm.data.AppDatabase
import com.example.mofilm.data.FilmProcess
import com.example.mofilm.data.FilmProcessRepository
import com.example.mofilm.data.FilmProcessWithScans
import com.example.mofilm.data.Scan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilmProcessViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FilmProcessRepository
    val allProcesses: StateFlow<List<FilmProcess>>
    val uniqueFilmTypes: StateFlow<List<String>>
    val uniqueDevelopers: StateFlow<List<String>>
    val processesWithScans: StateFlow<List<FilmProcessWithScans>>

    init {
        val dao = AppDatabase.getDatabase(application).filmProcessDao()
        repository = FilmProcessRepository(dao)
        allProcesses = repository.allProcesses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        uniqueFilmTypes = repository.uniqueFilmTypes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        uniqueDevelopers = repository.uniqueDevelopers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        processesWithScans = repository.processesWithScans.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addProcess(
        filmType: String,
        isPushPull: Boolean,
        pushPullValue: String?,
        developer: String,
        temperature: Double,
        developingTime: String,
        dilution: String,
        processDescription: String,
        comments: String
    ) {
        viewModelScope.launch {
            val newProcess = FilmProcess(
                date = System.currentTimeMillis(),
                filmType = filmType,
                isPushPull = isPushPull,
                pushPullValue = pushPullValue,
                developer = developer,
                temperature = temperature,
                developingTime = developingTime,
                dilution = dilution,
                processDescription = processDescription,
                comments = comments
            )
            repository.insert(newProcess)
        }
    }

    fun updateProcess(process: FilmProcess) {
        viewModelScope.launch {
            repository.update(process)
        }
    }

    fun deleteProcess(process: FilmProcess) {
        viewModelScope.launch {
            repository.delete(process)
        }
    }

    fun updateFilmType(oldType: String, newType: String) {
        viewModelScope.launch {
            repository.updateFilmType(oldType, newType)
        }
    }

    fun updateDeveloper(oldDev: String, newDev: String) {
        viewModelScope.launch {
            repository.updateDeveloper(oldDev, newDev)
        }
    }

    fun addScanToProcess(processId: Int, uri: String) {
        viewModelScope.launch {
            repository.insertScan(Scan(uri = uri, processId = processId))
        }
    }

    fun deleteScan(scan: Scan) {
        viewModelScope.launch {
            repository.deleteScan(scan)
        }
    }
}
