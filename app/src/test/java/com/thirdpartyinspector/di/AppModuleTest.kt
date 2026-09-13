package com.thirdpartyinspector.di

import com.thirdpartyinspector.scanner.PackageInfoModel
import com.thirdpartyinspector.scanner.RiskEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class AppModuleTest {
    @Test
    fun `provideRiskEngine behaves like a direct RiskEngine constructor`() {
        val packageInfo = PackageInfoModel(
            packageName = "com.example.suspicious",
            label = "Suspicious App",
            versionName = "1.0",
            versionCode = 1L,
            firstInstallTime = 0L,
            lastUpdateTime = 0L,
            installerPackageName = "com.unknown.installer",
            enabled = true,
            isSystemApp = false,
            hasLauncher = true,
            requestedPermissions = listOf(
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.SYSTEM_ALERT_WINDOW"
            )
        )

        val providedFindings = AppModule.provideRiskEngine().evaluatePackage(packageInfo)
        val directFindings = RiskEngine().evaluatePackage(packageInfo)

        assertEquals(
            directFindings.map { it.copy(id = "", timestamp = 0L) },
            providedFindings.map { it.copy(id = "", timestamp = 0L) }
        )
    }
}
