package com.thirdpartyinspector.scanner

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.thirdpartyinspector.data.model.Finding
import com.thirdpartyinspector.domain.model.FindingCategory
import com.thirdpartyinspector.domain.model.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.util.*

/**
 * Metadata about a file accessible to the app (via SAF or file APIs).
 */
data class FileMetadata(
    val uri: Uri?,
    val name: String,
    val path: String?, // best-effort human-readable path when available
    val size: Long?,
    val lastModified: Long?,
    val mimeType: String?,
    val sha256: String?
)

/**
 * FileSystemInspector scans user-selected directories (via SAF) or app-accessible storage
 * and produces Findings for suspicious files. It only reads files the app is legitimately allowed to access.
 */
class FileSystemInspector(private val context: Context) {

    // thresholds and settings
    private val largeFileThresholdBytes = 100L * 1024L * 1024L // 100 MB
    private val recentMs = 30L * 24 * 60 * 60 * 1000L // 30 days

    /**
     * Scan a tree URI obtained from the Storage Access Framework (e.g., ACTION_OPEN_DOCUMENT_TREE).
     * Returns findings for files that match suspicious rules. This will only access files the user granted
     * URI permission for.
     */
    suspend fun scanTreeUri(treeUri: Uri): List<Finding> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val findings = mutableListOf<Finding>()
        traverseDocumentFile(root, findings)
        findings
    }

    private fun traverseDocumentFile(dir: DocumentFile, findings: MutableList<Finding>) {
        val children = try { dir.listFiles() } catch (e: Exception) { arrayOf<DocumentFile>() }
        for (child in children) {
            try {
                if (child.isDirectory) {
                    traverseDocumentFile(child, findings)
                } else if (child.isFile) {
                    val meta = fetchMetadataForDocumentFile(child)
                    val fileFindings = FileSystemInspectorLogic.analyzeFile(meta, largeFileThresholdBytes, recentMs)
                    findings.addAll(fileFindings)
                }
            } catch (_: Throwable) {
                // skip unreadable entries
            }
        }
    }

    private fun fetchMetadataForDocumentFile(file: DocumentFile): FileMetadata {
        val uri = file.uri
        val name = file.name ?: ""
        val size = try { file.length() } catch (_: Exception) { null }
        val last = try { file.lastModified() } catch (_: Exception) { null }
        val mime = file.type ?: guessMimeTypeFromName(name)
        val sha = try { computeSha256ForUri(uri) } catch (_: Exception) { null }
        val path = uri?.toString()
        return FileMetadata(uri, name, path, size, last, mime, sha)
    }

    private fun guessMimeTypeFromName(name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        return if (ext.isEmpty()) null else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun computeSha256ForUri(uri: Uri?): String? {
        if (uri == null) return null
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri)
            input?.use { stream -> digestSha256(stream) }
        } catch (e: Exception) {
            null
        }
    }

    private fun digestSha256(stream: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8 * 1024)
        var read: Int
        while (true) {
            read = stream.read(buffer)
            if (read <= 0) break
            md.update(buffer, 0, read)
        }
        return md.digest().joinToString("") { String.format("%02x", it) }
    }
}

/**
 * Pure logic for analyzing FileMetadata and producing Findings. Extracted for unit testing.
 */
object FileSystemInspectorLogic {

    private val apkExtensions = setOf("apk", "apks", "xapk")
    private val executableExtensions = setOf("sh", "bin", "run", "elf")

    fun analyzeFile(meta: FileMetadata, largeFileThresholdBytes: Long, recentMs: Long): List<Finding> {
        val findings = mutableListOf<Finding>()
        val now = System.currentTimeMillis()

        val name = meta.name
        val lower = name.lowercase(Locale.US)
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)

        // Hidden file/directory
        if (name.startsWith('.')) {
            findings += makeFinding(
                category = FindingCategory.FILE,
                severity = Severity.LOW,
                title = "Hidden file",
                explanation = "File name starts with a dot, which is commonly used to hide files from casual browsing.",
                evidence = listOf("name=$name", "path=${meta.path}"),
                recommended = "Verify its purpose. If unused, consider removing.",
                packageName = null
            )
        }

        // APK/APKS/XAPK detection
        if (apkExtensions.contains(ext) || lower.endsWith(".apk")) {
            findings += makeFinding(
                category = FindingCategory.FILE,
                severity = Severity.MEDIUM,
                title = "APK or package file detected",
                explanation = "This file appears to be an Android package (APK/APKS/XAPK). Sideloaded packages can be used to install apps outside of app stores.",
                evidence = listOf("name=$name", "mime=${meta.mimeType}", "sha256=${meta.sha256 ?: "unavailable"}"),
                recommended = "Do not install unknown APKs. If you did not place this file intentionally, consider removing it.",
                packageName = null
            )
        }

        // Executable-like extensions
        if (executableExtensions.contains(ext)) {
            findings += makeFinding(
                category = FindingCategory.FILE,
                severity = Severity.MEDIUM,
                title = "File with executable-like extension",
                explanation = "This file has an extension commonly associated with executables or scripts.",
                evidence = listOf("name=$name", "extension=$ext"),
                recommended = "Inspect file contents before executing. Avoid running unknown scripts.",
                packageName = null
            )
        }

        // Recently modified
        if (meta.lastModified != null && (now - meta.lastModified) < recentMs) {
            findings += makeFinding(
                category = FindingCategory.FILE,
                severity = Severity.LOW,
                title = "Recently modified file",
                explanation = "This file has been modified recently.",
                evidence = listOf("lastModified=${meta.lastModified}"),
                recommended = "If unexpected, investigate the file and its origin.",
                packageName = null
            )
        }

        // Unusually large files
        if (meta.size != null && meta.size >= largeFileThresholdBytes) {
            findings += makeFinding(
                category = FindingCategory.FILE,
                severity = Severity.MEDIUM,
                title = "Large file detected",
                explanation = "This file is unusually large and may contain packaged content or media.",
                evidence = listOf("size=${meta.size}"),
                recommended = "Review the file to ensure it is expected. Consider moving large media to external storage or deleting if unnecessary.",
                packageName = null
            )
        }

        return findings
    }

    private fun makeFinding(
        category: FindingCategory,
        severity: Severity,
        title: String,
        explanation: String,
        evidence: List<String>,
        recommended: String,
        packageName: String?
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            category = category,
            severity = severity,
            confidence = when (severity) {
                Severity.CRITICAL -> 95
                Severity.HIGH -> 90
                Severity.MEDIUM -> 80
                Severity.LOW -> 70
                Severity.INFO -> 60
            },
            packageName = packageName,
            appLabel = null,
            title = title,
            explanation = explanation,
            evidence = evidence,
            recommendedAction = recommended,
            timestamp = System.currentTimeMillis()
        )
    }
}
