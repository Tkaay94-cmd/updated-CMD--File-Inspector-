package com.thirdpartyinspector.scanner

import org.junit.Assert.*
import org.junit.Test

class RootEnvironmentInspectorLogicTest {

    @Test
    fun `no indicators returns NOT_DETECTED`() {
        val result = RootEnvironmentInspectorLogic.evaluateIndicators(emptyList())
        assertEquals(RootPresence.NOT_DETECTED, result.presence)
        assertEquals(10, result.confidence)
    }

    @Test
    fun `single su indicator returns POSSIBLY_PRESENT`() {
        val result = RootEnvironmentInspectorLogic.evaluateIndicators(listOf("su_present:/system/xbin/su"))
        assertEquals(RootPresence.POSSIBLY_PRESENT, result.presence)
        assertTrue(result.confidence > 10)
    }

    @Test
    fun `su plus build tags escalate to CONFIRMED_INDICATOR`() {
        val result = RootEnvironmentInspectorLogic.evaluateIndicators(listOf("su_present:/system/xbin/su", "build_tags:test-keys"))
        assertEquals(RootPresence.CONFIRMED_INDICATOR, result.presence)
        assertEquals(95, result.confidence)
    }

    @Test
    fun `known root package alone is possibly present`() {
        val result = RootEnvironmentInspectorLogic.evaluateIndicators(listOf("known_root_pkg:com.topjohnwu.magisk"))
        assertEquals(RootPresence.POSSIBLY_PRESENT, result.presence)
    }

    @Test
    fun `multiple strong indicators produce confirmed`() {
        val result = RootEnvironmentInspectorLogic.evaluateIndicators(
            listOf("known_root_pkg:com.topjohnwu.magisk", "magisk_path_present:/sbin/.magisk", "su_present:/system/bin/su")
        )
        assertEquals(RootPresence.CONFIRMED_INDICATOR, result.presence)
        assertEquals(95, result.confidence)
    }
}
