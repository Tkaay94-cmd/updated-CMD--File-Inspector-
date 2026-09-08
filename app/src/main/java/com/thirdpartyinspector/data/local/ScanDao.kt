kotlin
package com.thirdpartyinspector.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thirdpartyinspector.data.local.entities.ScanEntity
import com.thirdpartyinspector.data.local.entities.FindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity)

    @Query("SELECT * FROM scans ORDER BY startTime DESC")
    fun getScans(): Flow<List<ScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<FindingEntity>)

    @Query("SELECT * FROM findings WHERE scanId = :scanId")
    fun getFindingsForScan(scanId: String): Flow<List<FindingEntity>>
}
