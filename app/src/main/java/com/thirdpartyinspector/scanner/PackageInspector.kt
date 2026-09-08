kotlin
package com.thirdpartyinspector.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

data class PackageInfoModel(
    val packageName: String,
    val label: String?,
    val versionName: String?,
    val versionCode: Long?,
    val firstInstallTime: Long?,
    val lastUpdateTime: Long?,
    val installerPackageName: String?,
    val enabled: Boolean,
    val isSystemApp: Boolean,
    val hasLauncher: Boolean,
    val requestedPermissions: List<String>?
)

class PackageInspector(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    /**
     * Enumerate packages legitimately visible to this app.
     * NOTE: On Android 11+ package visibility may limit results. We do NOT request QUERY_ALL_PACKAGES by default.
     */
    suspend fun listVisiblePackages(): List<PackageInfoModel> = withContext(Dispatchers.IO) {
        val packages: List<PackageInfo> = pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        val result = ArrayList<PackageInfoModel>(packages.size)
        for (p in packages) {
            val ai: ApplicationInfo = p.applicationInfo
            val label = try {
                ai.loadLabel(pm)?.toString()
            } catch (e: Throwable) {
                null
            }
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val hasLauncher = try {
                val intent = pm.getLaunchIntentForPackage(p.packageName)
                intent != null
            } catch (e: Throwable) {
                false
            }
            val installer = try {
                pm.getInstallerPackageName(p.packageName)
            } catch (e: Throwable) {
                null
            }
            val requestedPermissions = p.requestedPermissions?.toList()

            val versionCode: Long? = try {
                if (android.os.Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong()
            } catch (ignored: Exception) {
                null
            }

            result += PackageInfoModel(
                packageName = p.packageName,
                label = label,
                versionName = p.versionName,
                versionCode = versionCode,
                firstInstallTime = if (p.firstInstallTime > 0) p.firstInstallTime else null,
                lastUpdateTime = if (p.lastUpdateTime > 0) p.lastUpdateTime else null,
                installerPackageName = installer,
                enabled = ai.enabled,
                isSystemApp = isSystem,
                hasLauncher = hasLauncher,
                requestedPermissions = requestedPermissions
            )
        }
        result
    }
}
