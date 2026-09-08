kotlin
package com.thirdpartyinspector.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thirdpartyinspector.domain.model.Severity

@Entity(tableName = "findings")
data class FindingEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val category: String,
    val severity: String,
    val confidence: Int,
    val packageName: String?,
    val appLabel: String?,
    val title: String,
    val explanation: String,
    val evidenceJson: String, // JSON encoded list
    val recommendedAction: String,
    val timestamp: Long
)
