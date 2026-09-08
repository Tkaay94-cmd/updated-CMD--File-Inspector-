package com.thirdpartyinspector.scanner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Model representing a device administrator entry.
 */
data class DeviceAdminModel(
    val packageName: String,
    val componentName: String,
    val appLabel: String?,
    val isActive: Boolean,
    val isDeviceOwner: Boolean?,
    val isProfileOwner: Boolean?,
    val firstInstallTime: Long?,
    val installerPackageName: String?
)

/**
 * DeviceAdminInspector detects active device administrator applications, device owner and profile owner.
 * It uses public Android APIs and degrades gracefully when Android limits access.
 */
class DeviceAdminInspector(private val context: Context) {
    private val pm = context.packageManager

    /**
     * List active device admin components visible to this app.
     * This uses DevicePolicyManager.getActiveAdmins(). Access to this data is limited to public APIs.
     */
    suspend fun listActiveAdmins(): Result<List<DeviceAdminModel>> = withContext(Dispatchers.IO) {
        try {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
                ?: return@withContext Result.failure(Exception("DevicePolicyManager not available"))

            val active: List<ComponentName>? = try {
                @Suppress("DEPRECATION")
                dpm.activeAdmins
            } catch (e: SecurityException) {
                // Some devices may restrict access
                null
            }

            if (active.isNullOrEmpty()) return@withContext Result.success(emptyList())

            val list = mutableListOf<DeviceAdminModel>()
            for (comp in active) {
                try {
                    val pkg = comp.packageName
                    val component = comp.flattenToString()
                    val pkgInfo = try { pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())) } catch (e: Exception) { null }
                    val label = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0))?.toString() } catch (e: Exception) { null }
                    val firstInstall = try { pkgInfo?.firstInstallTime } catch (e: Exception) { null }
                    val installer = try { pm.getInstallerPackageName(pkg) } catch (e: Exception) { null }
                    val isDeviceOwner = try { dpm.isDeviceOwnerApp(pkg) } catch (e: Exception) { null }
                    val isProfileOwner = try { dpm.isProfileOwnerApp(pkg) } catch (e: Exception) { null }

                    list += DeviceAdminModel(
                        packageName = pkg,
                        componentName = component,
                        appLabel = label,
                        isActive = true,
                        isDeviceOwner = isDeviceOwner,
                        isProfileOwner = isProfileOwner,
                        firstInstallTime = if (firstInstall != null && firstInstall > 0) firstInstall else null,
                        installerPackageName = installer
                    )
                } catch (e: Exception) {
                    // Skip entries that cannot be inspected
                }
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Evaluate DeviceAdminModel entries and produce Findings using a testable logic implementation.
     */
    fun evaluateAdmins(admins: List<DeviceAdminModel>): List<com.thirdpartyinspector.data.model.Finding> {
        return DeviceAdminInspectorLogic.evaluateAdmins(admins)
    }
}

/**
 * Pure logic for evaluating device admin entries. Extracted to allow unit testing without Android runtime.
 */
object DeviceAdminInspectorLogic {
    private val recognizedInstallers = setOf(
        "com.android.vending",
        "com.android.vending:delivery",
        "com.amazon.venezia",
        "com.sec.android.app.samsungapps"
    )

    fun evaluateAdmins(admins: List<DeviceAdminModel>): List<com.thirdpartyinspector.data.model.Finding> {
        val findings = mutableListOf<com.thirdpartyinspector.data.model.Finding>()
        val now = System.currentTimeMillis()

        for (a in admins) {
            // Active device admin => MEDIUM
            findings += com.thirdpartyinspector.data.model.Finding(
                id = UUID.randomUUID().toString(),
                category = com.thirdpartyinspector.domain.model.FindingCategory.DEVICE_ADMIN,
                severity = com.thirdpartyinspector.domain.model.Severity.MEDIUM,
                confidence = 80,
                packageName = a.packageName,
                appLabel = a.appLabel,
                title = "Active device administrator",
                explanation = "This app is an active device administrator and can exercise device-level controls.",
                evidence = listOf("component=${a.componentName}", "isDeviceOwner=${a.isDeviceOwner}", "isProfileOwner=${a.isProfileOwner}"),
                recommendedAction = "If this app is not managed by your organization or you do not trust it, consider removing device admin privileges in Settings.",
                timestamp = now
            )

            // Device owner not from recognized store => HIGH
            if (a.isDeviceOwner == true) {
                val installerUnknown = a.installerPackageName == null || !recognizedInstallers.contains(a.installerPackageName)
                if (installerUnknown) {
                    findings += com.thirdpartyinspector.data.model.Finding(
                        id = UUID.randomUUID().toString(),
                        category = com.thirdpartyinspector.domain.model.FindingCategory.DEVICE_ADMIN,
                        severity = com.thirdpartyinspector.domain.model.Severity.HIGH,
                        confidence = 90,
                        packageName = a.packageName,
                        appLabel = a.appLabel,
                        title = "Device owner installed from unknown source",
                        explanation = "Device owner app was not installed from a recognized app store according to installer metadata.",
                        evidence = listOf("installer=${a.installerPackageName}"),
                        recommendedAction = "Verify the device owner and installation source. Consult your device administrator or factory reset if compromised.",
                        timestamp = now
                    )
                }
            }

            // Profile owner combination
            if (a.isProfileOwner == true) {
                findings += com.thirdpartyinspector.data.model.Finding(
                    id = UUID.randomUUID().toString(),
                    category = com.thirdpartyinspector.domain.model.FindingCategory.DEVICE_ADMIN,
                    severity = com.thirdpartyinspector.domain.model.Severity.HIGH,
                    confidence = 85,
                    packageName = a.packageName,
                    appLabel = a.appLabel,
                    title = "Profile owner",
                    explanation = "This application is the profile owner for a managed profile on the device.",
                    evidence = listOf("profileOwner=true"),
                    recommendedAction = "If you do not recognize this management profile, consult your administrator or remove the profile.",
                    timestamp = now
                )
            }
        }

        return findings
    }
}
