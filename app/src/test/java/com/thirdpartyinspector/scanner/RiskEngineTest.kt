package com.thirdpartyinspector.scanner

import com.thirdpartyinspector.data.model.Finding
import com.thirdpartyinspector.domain.model.FindingCategory
import com.thirdpartyinspector.domain.model.Severity
import org.junit.Assert.*
import org.junit.Test

class RiskEngineTest {
    private val engine = RiskEngine()

    @Test
    fun `recently installed produces low severity finding`() {
        val now = System.currentTimeMillis()
        val pkg = PackageInfoModel(
            packageName = "com.example.recent",
            label = "Recent App",
            versionName = "1.0",
            versionCode = 1L,
            firstInstallTime = now - 5L * 24 * 60 * 60 * 1000, // 5 days ago
            lastUpdateTime = now - 2L * 24 * 60 * 60 * 1000,
            installerPackageName = "com.android.vending",
            enabled = true,
            isSystemApp = false,
            hasLauncher = true,
            requestedPermissions = emptyList()
        )

        val findings = engine.evaluatePackage(pkg)
        assertTrue("Expect at least one finding", findings.isNotEmpty())
        val hasLow = findings.any { it.severity == Severity.LOW }
        assertTrue("Expected a LOW severity finding for recent install", hasLow)
    }

    @Test
    fun `accessibility plus overlay produces high severity finding`() {
        val now = System.currentTimeMillis()
        val pkg = PackageInfoModel(
            packageName = "com.example.bad",
            label = "Bad App",
            versionName = "1.2",
            versionCode = 2L,
            firstInstallTime = now - 100L * 24 * 60 * 60 * 1000, // old install
            lastUpdateTime = now - 1L * 24 * 60 * 60 * 1000,
            installerPackageName = "com.unknown.installer",
            enabled = true,
            isSystemApp = false,
            hasLauncher = false,
            requestedPermissions = listOf("android.permission.BIND_ACCESSIBILITY_SERVICE", "android.permission.SYSTEM_ALERT_WINDOW")
        )

        val findings = engine.evaluatePackage(pkg)
        // Expect at least one HIGH severity finding
        val hasHigh = findings.any { it.severity == Severity.HIGH }
        assertTrue("Expected HIGH severity when accessibility + overlay are present", hasHigh)
    }

    @Test
    fun `multiple sensitive permissions escalate to high`() {
        val now = System.currentTimeMillis()
        val pkg = PackageInfoModel(
            packageName = "com.example.sensitive",
            label = "Sensitive App",
            versionName = "3.0",
            versionCode = 3L,
            firstInstallTime = now - 200L * 24 * 60 * 60 * 1000,
            lastUpdateTime = now - 1L * 24 * 60 * 60 * 1000,
            installerPackageName = "com.android.vending",
            enabled = true,
            isSystemApp = false,
            hasLauncher = true,
            requestedPermissions = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION")
        )

        val findings = engine.evaluatePackage(pkg)
        val hasHigh = findings.any { it.severity == Severity.HIGH }
        assertTrue("Expected HIGH severity when multiple sensitive permissions present", hasHigh)
    }
}
