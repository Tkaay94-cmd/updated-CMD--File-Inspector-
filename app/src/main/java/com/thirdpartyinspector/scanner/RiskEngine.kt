package com.thirdpartyinspector.scanner

import com.thirdpartyinspector.data.model.Finding
import com.thirdpartyinspector.domain.model.FindingCategory
import com.thirdpartyinspector.domain.model.Severity
import java.util.*

/**
 * Enhanced RiskEngine with transparent rule-based scoring for permissions and combinations.
 */
class RiskEngine {
    private val recognizedInstallers = setOf(
        "com.android.vending", // Google Play
        "com.android.vending:delivery", // some variants
        "com.amazon.venezia",
        "com.sec.android.app.samsungapps"
    )

    fun evaluatePackage(pkg: PackageInfoModel): List<Finding> {
        val findings = mutableListOf<Finding>()
        val now = System.currentTimeMillis()

        // Rule: Recently installed => LOW
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val installedRecently = pkg.firstInstallTime != null && (now - pkg.firstInstallTime) < thirtyDaysMs
        if (installedRecently) {
            findings += Finding(
                id = UUID.randomUUID().toString(),
                category = FindingCategory.PACKAGE,
                severity = Severity.LOW,
                confidence = 80,
                packageName = pkg.packageName,
                appLabel = pkg.label,
                title = "Recently installed",
                explanation = "Application was installed within the last 30 days.",
                evidence = listOf("firstInstallTime=${pkg.firstInstallTime}"),
                recommendedAction = "If you don't recognize this app, review or uninstall it.",
                timestamp = now
            )
        }

        // Gather requested permissions set for easier checks
        val requested = pkg.requestedPermissions?.map { it.trim() }?.toSet() ?: emptySet()

        // High-risk permission categories
        val highRiskPerms = setOf(
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            "android.permission.BIND_VPN_SERVICE",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.SEND_SMS",
            "android.permission.READ_SMS",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        )

        val presentHighRisk = requested.intersect(highRiskPerms)

        // Rule: Accessibility + Overlay => HIGH
        val hasAccessibility = requested.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")
        val hasOverlay = requested.contains("android.permission.SYSTEM_ALERT_WINDOW")
        if (hasAccessibility && hasOverlay) {
            findings += Finding(
                id = UUID.randomUUID().toString(),
                category = FindingCategory.PERMISSION,
                severity = Severity.HIGH,
                confidence = 90,
                packageName = pkg.packageName,
                appLabel = pkg.label,
                title = "Accessibility service with overlay capability",
                explanation = "The app requests Accessibility service access together with overlay capability, which can allow extensive control and observation of the device UI.",
                evidence = listOf("permissions=${presentHighRisk.sorted()}", "installer=${pkg.installerPackageName}"),
                recommendedAction = "Review whether this app needs Accessibility and overlay. If unknown, consider disabling the Accessibility service for this app and uninstalling if untrusted.",
                timestamp = now
            )
        }

        // Rule: Unknown installer + overlay => MEDIUM
        val installerUnknown = pkg.installerPackageName == null || !recognizedInstallers.contains(pkg.installerPackageName)
        if (installerUnknown && hasOverlay) {
            findings += Finding(
                id = UUID.randomUUID().toString(),
                category = FindingCategory.PACKAGE,
                severity = Severity.MEDIUM,
                confidence = 75,
                packageName = pkg.packageName,
                appLabel = pkg.label,
                title = "Overlay permission with unknown installer",
                explanation = "The app can draw overlays and was not installed from a recognized app store according to installer metadata.",
                evidence = listOf("installer=${pkg.installerPackageName}", "permissions=${presentHighRisk.sorted()}"),
                recommendedAction = "If you do not recognize this app, consider removing overlay permission or uninstalling.",
                timestamp = now
            )
        }

        // Rule: Count of sensitive runtime permissions
        val sensitiveSet = setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.CALL_PHONE",
            "android.permission.SEND_SMS",
            "android.permission.READ_SMS"
        )
        val sensitivePresent = requested.intersect(sensitiveSet)
        if (sensitivePresent.isNotEmpty()) {
            val severity = when (sensitivePresent.size) {
                1 -> Severity.LOW
                2 -> Severity.MEDIUM
                else -> Severity.HIGH
            }
            val confidence = 60 + (sensitivePresent.size * 10)
            findings += Finding(
                id = UUID.randomUUID().toString(),
                category = FindingCategory.PERMISSION,
                severity = severity,
                confidence = confidence.coerceIn(0, 100),
                packageName = pkg.packageName,
                appLabel = pkg.label,
                title = "Sensitive runtime permissions requested",
                explanation = "The app requests sensitive permissions: ${sensitivePresent.sorted()}. Review whether the app needs these permissions.",
                evidence = listOf("permissions=${sensitivePresent.sorted()}"),
                recommendedAction = "Review permission usage in app settings; deny or uninstall if unnecessary.",
                timestamp = now
            )
        }

        // Additional rule examples could be added here: Device admin + unknown installer => HIGH, multiple strong indicators => CRITICAL, etc.

        // Aggregate rule: multiple strong indicators escalate severity
        val strongIndicators = findings.count { it.severity == Severity.HIGH || it.severity == Severity.CRITICAL }
        if (strongIndicators >= 2) {
            findings += Finding(
                id = UUID.randomUUID().toString(),
                category = FindingCategory.OTHER,
                severity = Severity.CRITICAL,
                confidence = 85,
                packageName = pkg.packageName,
                appLabel = pkg.label,
                title = "Multiple strong risk indicators",
                explanation = "This app exhibits multiple strong indicators (e.g., accessibility+overlay, multiple sensitive permissions) which together increase the risk.",
                evidence = listOf("high_indicators=$strongIndicators"),
                recommendedAction = "Investigate immediately and consider removing the app if untrusted.",
                timestamp = now
            )
        }

        return findings
    }
}
