package com.thirdpartyinspector.scanner

import org.junit.Assert.*
import org.junit.Test

class DeviceAdminInspectorTest {

    @Test
    fun `active device admin yields medium finding`() {
        val admin = DeviceAdminModel(
            packageName = "com.example.admin",
            componentName = "com.example.admin/.AdminReceiver",
            appLabel = "AdminApp",
            isActive = true,
            isDeviceOwner = false,
            isProfileOwner = false,
            firstInstallTime = System.currentTimeMillis() - 200L * 24 * 60 * 60 * 1000,
            installerPackageName = "com.android.vending"
        )
        val findings = DeviceAdminInspectorLogic.evaluateAdmins(listOf(admin))
        assertTrue(findings.any { it.category == com.thirdpartyinspector.domain.model.FindingCategory.DEVICE_ADMIN })
        assertTrue(findings.any { it.severity == com.thirdpartyinspector.domain.model.Severity.MEDIUM })
    }

    @Test
    fun `device owner with unknown installer yields high finding`() {
        val admin = DeviceAdminModel(
            packageName = "com.example.owner",
            componentName = "com.example.owner/.OwnerReceiver",
            appLabel = "OwnerApp",
            isActive = true,
            isDeviceOwner = true,
            isProfileOwner = false,
            firstInstallTime = System.currentTimeMillis() - 300L * 24 * 60 * 60 * 1000,
            installerPackageName = "com.unknown.installer"
        )
        val findings = DeviceAdminInspectorLogic.evaluateAdmins(listOf(admin))
        val hasHigh = findings.any { it.severity == com.thirdpartyinspector.domain.model.Severity.HIGH }
        assertTrue("Expected HIGH severity for device owner from unknown installer", hasHigh)
    }

    @Test
    fun `profile owner yields high finding`() {
        val admin = DeviceAdminModel(
            packageName = "com.example.profile",
            componentName = "com.example.profile/.ProfileReceiver",
            appLabel = "ProfileApp",
            isActive = true,
            isDeviceOwner = false,
            isProfileOwner = true,
            firstInstallTime = System.currentTimeMillis() - 50L * 24 * 60 * 60 * 1000,
            installerPackageName = "com.android.vending"
        )
        val findings = DeviceAdminInspectorLogic.evaluateAdmins(listOf(admin))
        val hasHigh = findings.any { it.severity == com.thirdpartyinspector.domain.model.Severity.HIGH }
        assertTrue("Expected HIGH severity for profile owner", hasHigh)
    }
}
