package com.thirdpartyinspector.data.repository

import com.thirdpartyinspector.data.local.ScanDao
import com.thirdpartyinspector.data.local.entities.ScanEntity
import com.thirdpartyinspector.data.local.entities.FindingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val dao: ScanDao
) {
    suspend fun saveScan(scan: ScanEntity, findings: List<FindingEntity>) {
        dao.insertScan(scan)
        if (findings.isNotEmpty()) dao.insertFindings(findings)
    }

    fun getScans(): Flow<List<ScanEntity>> = dao.getScans()

    fun getFindings(scanId: String): Flow<List<FindingEntity>> = dao.getFindingsForScan(scanId)
}
