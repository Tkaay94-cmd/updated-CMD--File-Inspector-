package com.thirdpartyinspector.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey val scanId: String,
    val startTime: Long,
    val finishTime: Long?,
    val androidVersion: String,
    val deviceModel: String,
    val totalRiskScore: Int
)
