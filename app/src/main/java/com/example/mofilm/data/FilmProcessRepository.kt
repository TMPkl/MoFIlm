package com.example.mofilm.data

import kotlinx.coroutines.flow.Flow

class FilmProcessRepository(private val filmProcessDao: FilmProcessDao) {
    val allProcesses: Flow<List<FilmProcess>> = filmProcessDao.getAllProcesses()
    val uniqueFilmTypes: Flow<List<String>> = filmProcessDao.getUniqueFilmTypes()
    val uniqueDevelopers: Flow<List<String>> = filmProcessDao.getUniqueDevelopers()
    val processesWithScans: Flow<List<FilmProcessWithScans>> = filmProcessDao.getProcessesWithScans()

    suspend fun insert(process: FilmProcess): Long {
        return filmProcessDao.insertProcess(process)
    }

    suspend fun update(process: FilmProcess) {
        filmProcessDao.updateProcess(process)
    }

    suspend fun delete(process: FilmProcess) {
        filmProcessDao.deleteProcess(process)
    }

    suspend fun updateFilmType(oldType: String, newType: String) {
        filmProcessDao.updateFilmType(oldType, newType)
    }

    suspend fun updateDeveloper(oldDev: String, newDev: String) {
        filmProcessDao.updateDeveloper(oldDev, newDev)
    }

    suspend fun insertScan(scan: Scan) {
        filmProcessDao.insertScan(scan)
    }

    suspend fun deleteScan(scan: Scan) {
        filmProcessDao.deleteScan(scan)
    }

    fun getScansForProcess(processId: Int): Flow<List<Scan>> {
        return filmProcessDao.getScansForProcess(processId)
    }
}
