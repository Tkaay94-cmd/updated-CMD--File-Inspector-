package com.thirdpartyinspector.data.model

import com.thirdpartyinspector.domain.model.FindingCategory
import com.thirdpartyinspector.domain.model.Severity

data class Finding(
    val id: String,
    val category: FindingCategory,
    val severity: Severity,
    val confidence: Int, // 0..100
    val packageName: String?,
    val appLabel: String?,
    val title: String,
    val explanation: String,
    val evidence: List<String>,
    val recommendedAction: String,
    val timestamp: Long
)
