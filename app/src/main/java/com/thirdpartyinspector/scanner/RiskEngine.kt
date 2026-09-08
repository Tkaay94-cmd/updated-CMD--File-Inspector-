kotlin
package com.thirdpartyinspector.scanner

import com.thirdpartyinspector.data.model.Finding
import com.thirdpartyinspector.domain.model.FindingCategory
import com.thirdpartyinspector.domain.model.Severity
import java.util.*

class RiskEngine {
    /**
     * Evaluate package and permission combinations and return zero or more Findings.
     * This is a simple, transparent rule-based engine. Rules should be easy to read and explain.
     */
    fun evaluatePackage(pkg: PackageInfoModel): List<Finding> {
        val findings = mutableListOf<Finding>()
        val now = System.currentTimeMillis()

        // Example: recently installed => LOW
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

        // Additional rules to be added here: unknown installer + overlay, accessibility + overlay, device admin outside store, etc.

        return findings
    }
}
