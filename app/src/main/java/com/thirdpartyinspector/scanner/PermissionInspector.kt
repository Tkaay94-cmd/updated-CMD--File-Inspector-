package com.thirdpartyinspector.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PermissionInspector analyzes a PackageInfoModel's declared/requested permissions and returns
 * a human-readable summary of high-risk permissions observed. It does not change device state.
 */
class PermissionInspector {
    suspend fun analyzePermissions(pkg: PackageInfoModel): PermissionAnalysis = withContext(Dispatchers.Default) {
        val requested = pkg.requestedPermissions?.map { it.trim() }?.toSet() ?: emptySet()
        val findings = mutableListOf<String>()

        if (requested.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
            findings += "Requests Accessibility service (BIND_ACCESSIBILITY_SERVICE)"
        }
        if (requested.contains("android.permission.SYSTEM_ALERT_WINDOW")) {
            findings += "Requests Overlay/System Alert Window (SYSTEM_ALERT_WINDOW)"
        }
        if (requested.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
            findings += "Can request package installation (REQUEST_INSTALL_PACKAGES)"
        }
        if (requested.contains("android.permission.PACKAGE_USAGE_STATS")) {
            findings += "Requests Usage Access (PACKAGE_USAGE_STATS)"
        }
        val sensitive = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_CONTACTS")
        val sensitivePresent = requested.intersect(sensitive)
        if (sensitivePresent.isNotEmpty()) {
            findings += "Requests sensitive permissions: ${sensitivePresent.sorted()}"
        }

        PermissionAnalysis(
            packageName = pkg.packageName,
            appLabel = pkg.label,
            observedPermissions = requested.sorted(),
            summaries = findings
        )
    }
}

data class PermissionAnalysis(
    val packageName: String,
    val appLabel: String?,
    val observedPermissions: List<String>,
    val summaries: List<String>
)
