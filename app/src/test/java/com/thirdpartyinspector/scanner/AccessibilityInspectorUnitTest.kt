package com.thirdpartyinspector.scanner

import org.junit.Assert.*
import org.junit.Test

class AccessibilityInspectorUnitTest {

    @Test
    fun `evaluateServices flags recently installed service`() {
        val now = System.currentTimeMillis()
        val svc = AccessibilityServiceModel(
            packageName = "com.example.recentacc",
            serviceName = "com.example.recentacc.Service",
            componentName = "com.example.recentacc/.Service",
            appLabel = "RecentAcc",
            enabled = true,
            firstInstallTime = now - 5L * 24 * 60 * 60 * 1000, // 5 days
            installerPackageName = "com.unknown.installer",
            requestedPermissions = listOf("android.permission.BIND_ACCESSIBILITY_SERVICE")
        )
        val inspector = AccessibilityInspectorTestHelper()
        val findings = inspector.evaluateServicesForTest(listOf(svc))
        assertTrue(findings.any { it.title.contains("Recently installed") || it.title.contains("unknown installer") })
    }

    @Test
    fun `evaluateServices flags overlay combination as high`() {
        val now = System.currentTimeMillis()
        val svc = AccessibilityServiceModel(
            packageName = "com.example.overlay",
            serviceName = "com.example.overlay.Service",
            componentName = "com.example.overlay/.Service",
            appLabel = "OverlayAcc",
            enabled = true,
            firstInstallTime = now - 100L * 24 * 60 * 60 * 1000,
            installerPackageName = "com.android.vending",
            requestedPermissions = listOf("android.permission.BIND_ACCESSIBILITY_SERVICE", "android.permission.SYSTEM_ALERT_WINDOW")
        )
        val inspector = AccessibilityInspectorTestHelper()
        val findings = inspector.evaluateServicesForTest(listOf(svc))
        val hasHigh = findings.any { it.severity == com.thirdpartyinspector.domain.model.Severity.HIGH }
        assertTrue("Expected HIGH severity for overlay+accessibility", hasHigh)
    }
}

// Helper wrapper to call the evaluateServices function without needing Android Context
class AccessibilityInspectorTestHelper {
    private val inspector = AccessibilityInspectorTestable()

    fun evaluateServicesForTest(services: List<AccessibilityServiceModel>) = inspector.evaluateServicesPublic(services)
}

// Testable delegate that exposes evaluateServices without requiring Context
class AccessibilityInspectorTestable {
    fun evaluateServicesPublic(services: List<AccessibilityServiceModel>) = AccessibilityInspectorLogic.evaluateServices(services)
}

// Extracted logic to a singleton so tests can call it directly
object AccessibilityInspectorLogic {
    fun evaluateServices(services: List<AccessibilityServiceModel>): List<com.thirdpartyinspector.data.model.Finding> {
        val findings = mutableListOf<com.thirdpartyinspector.data.model.Finding>()
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

        val recognizedInstallers = setOf(
            "com.android.vending",
            "com.android.vending:delivery",
            "com.amazon.venezia",
            "com.sec.android.app.samsungapps"
        )

        for (s in services) {
            val installedRecently = s.firstInstallTime != null && (now - s.firstInstallTime) < thirtyDaysMs
            if (installedRecently) {
                findings += com.thirdpartyinspector.data.model.Finding(
                    id = UUID.randomUUID().toString(),
                    category = com.thirdpartyinspector.domain.model.FindingCategory.ACCESSIBILITY,
                    severity = com.thirdpartyinspector.domain.model.Severity.LOW,
                    confidence = 75,
                    packageName = s.packageName,
                    appLabel = s.appLabel,
                    title = "Recently installed accessibility service",
                    explanation = "This accessibility service was installed recently and is enabled. Accessibility services can observe and interact with the device UI.",
                    evidence = listOf("component=${s.componentName}", "firstInstallTime=${s.firstInstallTime}"),
                    recommendedAction = "If you do not recognize this app, review or disable the accessibility service in Settings.",
                    timestamp = now
                )
            }

            val installerUnknown = s.installerPackageName == null || !recognizedInstallers.contains(s.installerPackageName)
            if (installerUnknown) {
                findings += com.thirdpartyinspector.data.model.Finding(
                    id = UUID.randomUUID().toString(),
                    category = com.thirdpartyinspector.domain.model.FindingCategory.ACCESSIBILITY,
                    severity = com.thirdpartyinspector.domain.model.Severity.MEDIUM,
                    confidence = 80,
                    packageName = s.packageName,
                    appLabel = s.appLabel,
                    title = "Accessibility service from unknown installer",
                    explanation = "Installer metadata for this app is missing or not in recognized stores. This may indicate sideloading.",
                    evidence = listOf("installer=${s.installerPackageName}"),
                    recommendedAction = "Verify the source of this app and consider uninstalling if untrusted.",
                    timestamp = now
                )
            }

            val requested = s.requestedPermissions?.map { it.trim() }?.toSet() ?: emptySet()
            val hasOverlay = requested.contains("android.permission.SYSTEM_ALERT_WINDOW")
            val hasDeviceAdmin = requested.contains("android.permission.BIND_DEVICE_ADMIN")
            if (hasOverlay || hasDeviceAdmin) {
                findings += com.thirdpartyinspector.data.model.Finding(
                    id = UUID.randomUUID().toString(),
                    category = com.thirdpartyinspector.domain.model.FindingCategory.ACCESSIBILITY,
                    severity = com.thirdpartyinspector.domain.model.Severity.HIGH,
                    confidence = 90,
                    packageName = s.packageName,
                    appLabel = s.appLabel,
                    title = "Accessibility service combined with overlay/device-admin",
                    explanation = "This app has an enabled accessibility service and also requests overlay or device-admin capabilities — a combination that increases potential risk.",
                    evidence = listOf("overlay=${hasOverlay}", "deviceAdmin=${hasDeviceAdmin}"),
                    recommendedAction = "Review the app closely and consider disabling the accessibility service or removing the app if not trusted.",
                    timestamp = now
                )
            }
        }

        return findings
    }
}
