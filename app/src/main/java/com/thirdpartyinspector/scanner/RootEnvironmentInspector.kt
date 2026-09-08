package com.thirdpartyinspector.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.thirdpartyinspector.data.model.Finding
import com.thirdpartyinspector.domain.model.FindingCategory
import com.thirdpartyinspector.domain.model.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

enum class RootPresence {
    NOT_DETECTED,
    POSSIBLY_PRESENT,
    CONFIRMED_INDICATOR
}

data class RootCheckResult(
    val presence: RootPresence,
    val confidence: Int,
    val indicators: List<String>
)

/**
 * RootEnvironmentInspector performs defensive checks for common root indicators using only
 * legitimate and read-only operations. It will not attempt to gain root or execute privileged commands.
 */
class RootEnvironmentInspector(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    // Common paths where "su" or root artifacts may appear. We only probe existence.
    private val suPaths = listOf(
        "/system/xbin/su",
        "/system/bin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/su/bin/su"
    )

    private val magiskPaths = listOf(
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/cache/.magisk"
    )

    private val knownRootPackages = listOf(
        "com.topjohnwu.magisk",
        "com.kingroot.kinguser",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser"
    )

    suspend fun checkForRoot(): RootCheckResult = withContext(Dispatchers.IO) {
        val indicators = mutableListOf<String>()

        // Check su paths defensively
        for (p in suPaths) {
            try {
                val f = File(p)
                if (f.exists()) {
                    indicators += "su_present:$p"
                }
            } catch (_: Exception) {
                // ignore inaccessible
            }
        }

        // Check known magisk paths
        for (p in magiskPaths) {
            try {
                val f = File(p)
                if (f.exists()) {
                    indicators += "magisk_path_present:$p"
                }
            } catch (_: Exception) {
            }
        }

        // Check Build.TAGS for test-keys
        try {
            val tags = Build.TAGS
            if (!tags.isNullOrEmpty() && tags.contains("test-keys")) {
                indicators += "build_tags:test-keys"
            }
        } catch (_: Exception) {
        }

        // Check for known root management packages visible to this app
        for (pkg in knownRootPackages) {
            try {
                val info = pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
                if (info != null) {
                    indicators += "known_root_pkg:$pkg"
                }
            } catch (_: Exception) {
                // package not found or not visible
            }
        }

        // Evaluate indicators using testable logic
        val result = RootEnvironmentInspectorLogic.evaluateIndicators(indicators)

        // Build a human-friendly confidence integer
        val conf = result.confidence
        result.copy(confidence = conf)
    }
}

/**
 * Pure, testable logic to interpret observed root indicators and produce a RootCheckResult.
 * Rules are transparent and conservative.
 */
object RootEnvironmentInspectorLogic {
    // Weights for different indicators (higher means stronger evidence)
    private val weights = mapOf(
        "su_present" to 2.0,
        "magisk_path_present" to 2.0,
        "known_root_pkg" to 1.5,
        "build_tags" to 1.0
    )

    /**
     * Evaluate a list of indicator strings of the form "type:value" and return a RootCheckResult.
     * Interpretation rules:
     * - Sum indicator weights; if total >= 3.0 => CONFIRMED_INDICATOR
     * - If total > 0 => POSSIBLY_PRESENT
     * - If total == 0 => NOT_DETECTED
     * Confidence is proportional to the total weight (scaled to 0..100).
     */
    fun evaluateIndicators(indicators: List<String>): RootCheckResult {
        if (indicators.isEmpty()) {
            return RootCheckResult(RootPresence.NOT_DETECTED, 10, indicators)
        }

        var totalWeight = 0.0
        for (ind in indicators) {
            val type = ind.substringBefore(':', ind)
            when {
                type.startsWith("su_present") -> totalWeight += weights["su_present"] ?: 0.0
                type.startsWith("magisk_path_present") -> totalWeight += weights["magisk_path_present"] ?: 0.0
                type.startsWith("known_root_pkg") -> totalWeight += weights["known_root_pkg"] ?: 0.0
                type.startsWith("build_tags") -> totalWeight += weights["build_tags"] ?: 0.0
                else -> totalWeight += 0.5 // unknown indicator, low weight
            }
        }

        val presence = when {
            totalWeight >= 3.0 -> RootPresence.CONFIRMED_INDICATOR
            totalWeight > 0.0 -> RootPresence.POSSIBLY_PRESENT
            else -> RootPresence.NOT_DETECTED
        }

        // Scale confidence: weight 0 -> 10, weight >=3 => 95, else scale linearly
        val confidence = when {
            totalWeight >= 3.0 -> 95
            totalWeight <= 0.0 -> 10
            else -> (10 + ((totalWeight / 3.0) * 85)).toInt().coerceIn(11, 94)
        }

        return RootCheckResult(presence, confidence, indicators)
    }
}
