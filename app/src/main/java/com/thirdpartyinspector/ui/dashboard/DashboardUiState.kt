package com.thirdpartyinspector.ui.dashboard

import com.thirdpartyinspector.data.local.entities.FindingEntity
import com.thirdpartyinspector.data.local.entities.ScanEntity
import com.thirdpartyinspector.domain.model.Severity
import java.util.Locale

/**
 * The state rendered by the dashboard. It deliberately contains display-ready values only so
 * Compose does not need to know about Room or scanner implementation details.
 */
data class DashboardUiState(
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val statusMessage: String? = null,
    val lastScan: ScanSummary? = null,
    val severityCounts: SeverityCounts = SeverityCounts(),
    val recentFindings: List<RecentFinding> = emptyList()
) {
    val isScanning: Boolean
        get() = scanStatus == ScanStatus.RUNNING
}

enum class ScanStatus {
    IDLE,
    RUNNING,
    FAILED
}

data class ScanSummary(
    val scanId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val riskScore: Int,
    val findingCount: Int,
    val deviceModel: String,
    val androidVersion: String
)

data class SeverityCounts(
    val critical: Int = 0,
    val high: Int = 0,
    val medium: Int = 0,
    val low: Int = 0,
    val info: Int = 0
) {
    val total: Int
        get() = critical + high + medium + low + info

    fun countFor(severity: Severity): Int = when (severity) {
        Severity.CRITICAL -> critical
        Severity.HIGH -> high
        Severity.MEDIUM -> medium
        Severity.LOW -> low
        Severity.INFO -> info
    }
}

data class RecentFinding(
    val id: String,
    val title: String,
    val sourceLabel: String?,
    val category: String,
    val severity: Severity,
    val timestamp: Long
)

/**
 * Pure transformation layer for persisted scan records. Unknown values are treated as INFO so a
 * future or malformed database value cannot prevent the user from opening the dashboard.
 */
object DashboardStateMapper {
    private const val RECENT_FINDING_LIMIT = 5

    fun toUiState(
        scans: List<ScanEntity>,
        findings: List<FindingEntity>,
        scanStatus: ScanStatus,
        statusMessage: String? = null
    ): DashboardUiState {
        val latestScan = scans.maxByOrNull { it.startTime }
        if (latestScan == null) {
            return DashboardUiState(
                scanStatus = scanStatus,
                statusMessage = statusMessage
            )
        }

        val latestFindings = findings
            .filter { it.scanId == latestScan.scanId }
            .sortedByDescending { it.timestamp }

        return DashboardUiState(
            scanStatus = scanStatus,
            statusMessage = statusMessage,
            lastScan = ScanSummary(
                scanId = latestScan.scanId,
                startedAt = latestScan.startTime,
                finishedAt = latestScan.finishTime,
                riskScore = latestScan.totalRiskScore,
                findingCount = latestFindings.size,
                deviceModel = latestScan.deviceModel,
                androidVersion = latestScan.androidVersion
            ),
            severityCounts = latestFindings.toSeverityCounts(),
            recentFindings = latestFindings
                .take(RECENT_FINDING_LIMIT)
                .map { finding ->
                    RecentFinding(
                        id = finding.id,
                        title = finding.title,
                        sourceLabel = finding.appLabel ?: finding.packageName,
                        category = finding.category.toDisplayLabel(),
                        severity = finding.severity.toSeverity(),
                        timestamp = finding.timestamp
                    )
                }
        )
    }

    private fun List<FindingEntity>.toSeverityCounts(): SeverityCounts {
        return SeverityCounts(
            critical = count { it.severity.toSeverity() == Severity.CRITICAL },
            high = count { it.severity.toSeverity() == Severity.HIGH },
            medium = count { it.severity.toSeverity() == Severity.MEDIUM },
            low = count { it.severity.toSeverity() == Severity.LOW },
            info = count { it.severity.toSeverity() == Severity.INFO }
        )
    }

    private fun String.toSeverity(): Severity = runCatching {
        Severity.valueOf(uppercase(Locale.US))
    }.getOrDefault(Severity.INFO)

    private fun String.toDisplayLabel(): String =
        lowercase(Locale.US)
            .split('_')
            .joinToString(" ") { token -> token.replaceFirstChar { it.titlecase(Locale.US) } }
}
