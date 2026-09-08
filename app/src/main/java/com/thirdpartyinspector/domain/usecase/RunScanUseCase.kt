package com.thirdpartyinspector.domain.usecase

import android.net.Uri
import android.os.Build
import com.thirdpartyinspector.data.local.entities.FindingEntity
import com.thirdpartyinspector.data.local.entities.ScanEntity
import com.thirdpartyinspector.data.model.Finding
import com.thirdpartyinspector.data.repository.ScanRepository
import com.thirdpartyinspector.domain.model.Severity
import com.thirdpartyinspector.scanner.*
import org.json.JSONArray
import java.util.*
import javax.inject.Inject

/**
 * Orchestrates a full scan using the available inspectors and persists the results via ScanRepository.
 */
class RunScanUseCase @Inject constructor(
    private val packageInspector: PackageInspector,
    private val permissionInspector: PermissionInspector,
    private val accessibilityInspector: AccessibilityInspector,
    private val deviceAdminInspector: DeviceAdminInspector,
    private val rootEnvironmentInspector: RootEnvironmentInspector,
    private val fileSystemInspector: FileSystemInspector,
    private val riskEngine: RiskEngine,
    private val repository: ScanRepository
) {

    suspend fun runScan(treeUriStrings: List<String>? = null): String {
        val scanId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()
        val findings = mutableListOf<Finding>()

        // 1. Packages
        val packages = packageInspector.listVisiblePackages()
        for (pkg in packages) {
            // Permission-based findings via PermissionInspector are included by RiskEngine rules where applicable.
            val pkgFindings = riskEngine.evaluatePackage(pkg)
            findings.addAll(pkgFindings)
        }

        // 2. Accessibility services
        val accServices = try { accessibilityInspector.listEnabledServices() } catch (e: Exception) { emptyList() }
        findings.addAll(accessibilityInspector.evaluateServices(accServices))

        // 3. Device admin
        val adminResult = deviceAdminInspector.listActiveAdmins()
        if (adminResult.isSuccess) {
            findings.addAll(deviceAdminInspector.evaluateAdmins(adminResult.getOrDefault(emptyList())))
        } else {
            // Record a finding explaining inability to access admin data
            findings.add(Finding(
                id = UUID.randomUUID().toString(),
                category = com.thirdpartyinspector.domain.model.FindingCategory.DEVICE_ADMIN,
                severity = Severity.INFO,
                confidence = 60,
                packageName = null,
                appLabel = null,
                title = "Device admin info not available",
                explanation = "Android prevented access to DevicePolicyManager details. The app cannot enumerate device admin entries on this device or with current permissions.",
                evidence = listOf(adminResult.exceptionOrNull()?.message ?: "access_denied"),
                recommendedAction = "Check device policies or run the app with appropriate privileges.",
                timestamp = System.currentTimeMillis()
            ))
        }

        // 4. Root environment
        val rootResult = try { rootEnvironmentInspector.checkForRoot() } catch (e: Exception) { null }
        if (rootResult != null && rootResult.presence != RootPresence.NOT_DETECTED) {
            findings.add(Finding(
                id = UUID.randomUUID().toString(),
                category = com.thirdpartyinspector.domain.model.FindingCategory.ROOT,
                severity = when (rootResult.presence) {
                    RootPresence.CONFIRMED_INDICATOR -> Severity.HIGH
                    RootPresence.POSSIBLY_PRESENT -> Severity.MEDIUM
                    else -> Severity.INFO
                },
                confidence = rootResult.confidence,
                packageName = null,
                appLabel = null,
                title = "Root indicators detected: ${rootResult.presence}",
                explanation = "Observed indicators: ${rootResult.indicators}",
                evidence = rootResult.indicators,
                recommendedAction = "If root is unexpected, investigate device integrity and avoid installing sensitive apps.",
                timestamp = System.currentTimeMillis()
            ))
        }

        // 5. File system (if user provided tree URIs)
        if (!treeUriStrings.isNullOrEmpty()) {
            for (s in treeUriStrings) {
                try {
                    val uri = Uri.parse(s)
                    val fileFindings = fileSystemInspector.scanTreeUri(uri)
                    findings.addAll(fileFindings)
                } catch (e: Exception) {
                    // add info finding about inability to scan this URI
                    findings.add(Finding(
                        id = UUID.randomUUID().toString(),
                        category = com.thirdpartyinspector.domain.model.FindingCategory.FILE,
                        severity = Severity.INFO,
                        confidence = 50,
                        packageName = null,
                        appLabel = null,
                        title = "Could not scan provided URI",
                        explanation = "Failed to access or scan user-provided URI: $s",
                        evidence = listOf(e.message ?: "unknown_error"),
                        recommendedAction = "Ensure the app has persisted permissions to the selected directory.",
                        timestamp = System.currentTimeMillis()
                    ))
                }
            }
        }

        val finishTime = System.currentTimeMillis()

        // Compute a simple total risk score
        val totalRiskScore = computeTotalRiskScore(findings)

        // Persist scan and findings
        val scanEntity = ScanEntity(
            scanId = scanId,
            startTime = startTime,
            finishTime = finishTime,
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            deviceModel = Build.MODEL ?: "unknown",
            totalRiskScore = totalRiskScore
        )

        val findingEntities = findings.map { f ->
            FindingEntity(
                id = f.id,
                scanId = scanId,
                category = f.category.name,
                severity = f.severity.name,
                confidence = f.confidence,
                packageName = f.packageName,
                appLabel = f.appLabel,
                title = f.title,
                explanation = f.explanation,
                evidenceJson = JSONArray(f.evidence).toString(),
                recommendedAction = f.recommendedAction,
                timestamp = f.timestamp
            )
        }

        repository.saveScan(scanEntity, findingEntities)

        return scanId
    }

    private fun computeTotalRiskScore(findings: List<Finding>): Int {
        var score = 0.0
        for (f in findings) {
            val weight = when (f.severity) {
                Severity.CRITICAL -> 5.0
                Severity.HIGH -> 3.0
                Severity.MEDIUM -> 1.5
                Severity.LOW -> 1.0
                Severity.INFO -> 0.5
            }
            score += weight * (f.confidence / 100.0)
        }
        return score.times(10).toInt() // scale to integer
    }
}
