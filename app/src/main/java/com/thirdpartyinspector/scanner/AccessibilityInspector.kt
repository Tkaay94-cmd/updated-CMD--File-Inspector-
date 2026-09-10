package com.thirdpartyinspector.scanner

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Model representing an enabled accessibility service discovered on the device.
 */
data class AccessibilityServiceModel(
    val packageName: String,
    val serviceName: String,
    val componentName: String,
    val appLabel: String?,
    val enabled: Boolean,
    val firstInstallTime: Long?,
    val installerPackageName: String?,
    val requestedPermissions: List<String>?
)

/**
 * AccessibilityInspector detects enabled accessibility services using legitimate Android APIs
 * and produces findings for review. It does NOT modify device state.
 */
class AccessibilityInspector(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    private val recognizedInstallers = setOf(
        "com.android.vending",
        "com.android.vending:delivery",
        "com.amazon.venezia",
        "com.sec.android.app.samsungapps"
    )

    /**
     * Returns a list of enabled accessibility services visible to this application.
     * This uses Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES which does not require special permissions.
     */
    suspend fun listEnabledServices(): List<AccessibilityServiceModel> = withContext(Dispatchers.IO) {
        val enabledServices = mutableListOf<AccessibilityServiceModel>()
        val flat = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } catch (e: Exception) {
            null
        }
        if (flat.isNullOrEmpty()) return@withContext enabledServices

        val parts = flat.split(":")
        for (part in parts) {
            try {
                val comp = ComponentName.unflattenFromString(part) ?: continue
                val pkgName = comp.packageName ?: continue
                val service = comp.className ?: ""

                // Attempt to get package metadata where available
                val pkgInfo: PackageInfo? = try {
                    pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                } catch (e: Exception) {
                    null
                }

                val ai: ApplicationInfo? = try {
                    pkgInfo?.applicationInfo ?: pm.getApplicationInfo(pkgName, 0)
                } catch (e: Exception) {
                    null
                }

                val label = try { ai?.loadLabel(pm)?.toString() } catch (e: Exception) { null }
                val firstInstall = try { pkgInfo?.firstInstallTime } catch (e: Exception) { null }
                val installer = try { pm.getInstallerPackageName(pkgName) } catch (e: Exception) { null }
                val requested = pkgInfo?.requestedPermissions?.toList()

                enabledServices += AccessibilityServiceModel(
                    packageName = pkgName,
                    serviceName = service,
                    componentName = part,
                    appLabel = label,
                    enabled = true,
                    firstInstallTime = if (firstInstall != null && firstInstall > 0) firstInstall else null,
                    installerPackageName = installer,
                    requestedPermissions = requested
                )
            } catch (_: Throwable) {
                // ignore malformed entries
            }
        }
        enabledServices
    }

    /**
     * Evaluate discovered accessibility services and produce Findings for review.
     * Flags:
     * - recently installed => LOW
     * - unknown or sideloaded installer => MEDIUM
     * - combined with overlay or device admin => HIGH
     */
    fun evaluateServices(services: List<AccessibilityServiceModel>): List<com.thirdpartyinspector.data.model.Finding> {
        val findings = mutableListOf<com.thirdpartyinspector.data.model.Finding>()
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

        for (s in services) {
            // Recently installed
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

            // Unknown or sideloaded installer
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

            // Combined checks: overlay or device admin + accessibility => HIGH
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
