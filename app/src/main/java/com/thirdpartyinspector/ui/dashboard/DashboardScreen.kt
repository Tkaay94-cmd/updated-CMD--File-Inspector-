package com.thirdpartyinspector.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thirdpartyinspector.domain.model.Severity
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardContent(
        uiState = uiState,
        onScanNow = viewModel::runScan
    )
}

@Composable
internal fun DashboardContent(
    uiState: DashboardUiState,
    onScanNow: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard-screen"),
        topBar = { DashboardHeader() },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScanStatusCard(
                    uiState = uiState,
                    onScanNow = onScanNow
                )
            }

            if (uiState.lastScan != null) {
                item {
                    FindingsSummaryCard(
                        summary = uiState.lastScan,
                        severityCounts = uiState.severityCounts
                    )
                }

                item {
                    Text(
                        text = "Recent findings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("dashboard-recent-findings-title")
                    )
                }

                if (uiState.recentFindings.isEmpty()) {
                    item { NoFindingsCard() }
                } else {
                    items(
                        items = uiState.recentFindings,
                        key = { it.id }
                    ) { finding ->
                        FindingCard(finding)
                    }
                }
            } else if (!uiState.isScanning) {
                item { FirstRunCard() }
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "ThirdParty Inspector",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dashboard-title")
                )
                Text(
                    text = "On-device security review",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScanStatusCard(
    uiState: DashboardUiState,
    onScanNow: () -> Unit
) {
    val status = when (uiState.scanStatus) {
        ScanStatus.RUNNING -> "Scanning"
        ScanStatus.FAILED -> "Scan needs attention"
        ScanStatus.IDLE -> if (uiState.lastScan == null) "Ready for a first scan" else "Results are up to date"
    }
    val message = uiState.statusMessage ?: when {
        uiState.isScanning -> "Reviewing available package, accessibility, device-admin, and root indicators."
        uiState.lastScan != null -> "Last scan ${formatTime(uiState.lastScan.finishedAt ?: uiState.lastScan.startedAt)}."
        else -> "Run a local inspection to create your first saved summary."
    }
    val statusColor = when (uiState.scanStatus) {
        ScanStatus.RUNNING -> MaterialTheme.colorScheme.primary
        ScanStatus.FAILED -> MaterialTheme.colorScheme.error
        ScanStatus.IDLE -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard-scan-status"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { stateDescription = status }
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onScanNow,
                enabled = !uiState.isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard-scan-button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uiState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Scanning…")
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan now")
                }
            }
        }
    }
}

@Composable
private fun FindingsSummaryCard(
    summary: ScanSummary,
    severityCounts: SeverityCounts
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard-findings-summary"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Latest scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Risk score",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.riskScore.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = riskScoreColor(summary.riskScore)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${summary.findingCount} finding${if (summary.findingCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatTime(summary.finishedAt ?: summary.startedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Severity breakdown",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SeverityMetric("Critical", severityCounts.critical, Severity.CRITICAL)
                SeverityMetric("High", severityCounts.high, Severity.HIGH)
                SeverityMetric("Medium", severityCounts.medium, Severity.MEDIUM)
                SeverityMetric("Low", severityCounts.low, Severity.LOW)
                SeverityMetric("Info", severityCounts.info, Severity.INFO)
            }
        }
    }
}

@Composable
private fun SeverityMetric(label: String, count: Int, severity: Severity) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.wrapContentWidth()
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = severityColor(severity)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FindingCard(finding: RecentFinding) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard-finding-${finding.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(top = 3.dp)
                    .clip(CircleShape)
                    .background(severityColor(finding.severity))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = finding.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val metadata = listOfNotNull(finding.sourceLabel, finding.category).joinToString(" • ")
                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            SeverityBadge(finding.severity)
        }
    }
}

@Composable
private fun SeverityBadge(severity: Severity) {
    Surface(
        color = severityColor(severity).copy(alpha = 0.15f),
        contentColor = severityColor(severity),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = severity.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FirstRunCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "What this scan checks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Visible app metadata, requested permissions, enabled accessibility services, active device-admin entries, and conservative root indicators. Results stay in this app’s local database.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoFindingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = "No findings were recorded in the latest scan.",
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun riskScoreColor(score: Int): Color = when {
    score >= 70 -> MaterialTheme.colorScheme.error
    score >= 35 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun severityColor(severity: Severity): Color = when (severity) {
    Severity.CRITICAL -> MaterialTheme.colorScheme.error
    Severity.HIGH -> Color(0xFFB3261E)
    Severity.MEDIUM -> Color(0xFF9A6700)
    Severity.LOW -> MaterialTheme.colorScheme.tertiary
    Severity.INFO -> MaterialTheme.colorScheme.primary
}

private fun formatTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
