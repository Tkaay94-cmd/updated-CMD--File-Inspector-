package com.thirdpartyinspector.ui.dashboard

import com.thirdpartyinspector.data.local.entities.FindingEntity
import com.thirdpartyinspector.data.local.entities.ScanEntity
import com.thirdpartyinspector.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardStateMapperTest {

    @Test
    fun `latest scan drives summary counts and the five newest findings`() {
        val olderScan = scan(id = "older", startedAt = 100L, score = 3)
        val latestScan = scan(id = "latest", startedAt = 200L, score = 42)
        val latestFindings = listOf(
            finding(id = "critical", scanId = "latest", severity = "CRITICAL", timestamp = 6L),
            finding(id = "high", scanId = "latest", severity = "HIGH", timestamp = 5L),
            finding(id = "medium", scanId = "latest", severity = "MEDIUM", timestamp = 4L),
            finding(id = "low", scanId = "latest", severity = "LOW", timestamp = 3L),
            finding(id = "info", scanId = "latest", severity = "INFO", timestamp = 2L),
            finding(id = "older-info", scanId = "latest", severity = "INFO", timestamp = 1L)
        )

        val state = DashboardStateMapper.toUiState(
            scans = listOf(olderScan, latestScan),
            findings = latestFindings + finding(id = "old", scanId = "older", severity = "HIGH", timestamp = 99L),
            scanStatus = ScanStatus.IDLE
        )

        assertEquals("latest", state.lastScan?.scanId)
        assertEquals(42, state.lastScan?.riskScore)
        assertEquals(6, state.lastScan?.findingCount)
        assertEquals(1, state.severityCounts.critical)
        assertEquals(1, state.severityCounts.high)
        assertEquals(1, state.severityCounts.medium)
        assertEquals(1, state.severityCounts.low)
        assertEquals(2, state.severityCounts.info)
        assertEquals(listOf("critical", "high", "medium", "low", "info"), state.recentFindings.map { it.id })
    }

    @Test
    fun `unknown persisted severity is displayed conservatively as info`() {
        val state = DashboardStateMapper.toUiState(
            scans = listOf(scan(id = "latest", startedAt = 200L, score = 0)),
            findings = listOf(finding(id = "future", scanId = "latest", severity = "FUTURE_LEVEL", timestamp = 1L)),
            scanStatus = ScanStatus.FAILED,
            statusMessage = "Scan could not finish. Please try again."
        )

        assertEquals(1, state.severityCounts.info)
        assertEquals(Severity.INFO, state.recentFindings.single().severity)
        assertEquals("Scan could not finish. Please try again.", state.statusMessage)
        assertEquals(ScanStatus.FAILED, state.scanStatus)
    }

    @Test
    fun `no persisted scans shows empty dashboard state`() {
        val state = DashboardStateMapper.toUiState(
            scans = emptyList(),
            findings = listOf(finding(id = "orphan", scanId = "missing", severity = "HIGH", timestamp = 1L)),
            scanStatus = ScanStatus.RUNNING,
            statusMessage = "Inspecting device metadata…"
        )

        assertNull(state.lastScan)
        assertEquals(0, state.severityCounts.total)
        assertEquals(emptyList<RecentFinding>(), state.recentFindings)
        assertEquals(ScanStatus.RUNNING, state.scanStatus)
    }

    private fun scan(id: String, startedAt: Long, score: Int) = ScanEntity(
        scanId = id,
        startTime = startedAt,
        finishTime = startedAt + 10L,
        androidVersion = "14",
        deviceModel = "Test Device",
        totalRiskScore = score
    )

    private fun finding(id: String, scanId: String, severity: String, timestamp: Long) = FindingEntity(
        id = id,
        scanId = scanId,
        category = "PERMISSION",
        severity = severity,
        confidence = 80,
        packageName = "com.example.test",
        appLabel = "Test App",
        title = id,
        explanation = "Test finding",
        evidenceJson = "[]",
        recommendedAction = "Review",
        timestamp = timestamp
    )
}
