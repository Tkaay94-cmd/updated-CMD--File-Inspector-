package com.thirdpartyinspector.scanner

import org.junit.Assert.*
import org.junit.Test

class FileSystemInspectorLogicTest {

    @Test
    fun `hidden file is detected as low severity`() {
        val meta = FileMetadata(
            uri = null,
            name = ".hiddenfile",
            path = "/storage/emulated/0/.hiddenfile",
            size = 1024L,
            lastModified = System.currentTimeMillis() - 1000L,
            mimeType = "text/plain",
            sha256 = null
        )
        val findings = FileSystemInspectorLogic.analyzeFile(meta, largeFileThresholdBytes = 100L * 1024L * 1024L, recentMs = 30L * 24 * 60 * 60 * 1000L)
        assertTrue(findings.any { it.title.contains("Hidden file") })
    }

    @Test
    fun `apk file is detected as medium severity`() {
        val meta = FileMetadata(
            uri = null,
            name = "payload.apk",
            path = "/storage/emulated/0/Download/payload.apk",
            size = 5L * 1024L * 1024L,
            lastModified = System.currentTimeMillis() - 1000L,
            mimeType = "application/vnd.android.package-archive",
            sha256 = "deadbeef"
        )
        val findings = FileSystemInspectorLogic.analyzeFile(meta, largeFileThresholdBytes = 100L * 1024L * 1024L, recentMs = 30L * 24 * 60 * 60 * 1000L)
        assertTrue(findings.any { it.title.contains("APK or package file") })
    }

    @Test
    fun `large file is detected as medium severity`() {
        val meta = FileMetadata(
            uri = null,
            name = "video.mp4",
            path = "/storage/emulated/0/Movies/video.mp4",
            size = 200L * 1024L * 1024L,
            lastModified = System.currentTimeMillis() - 1000L,
            mimeType = "video/mp4",
            sha256 = null
        )
        val findings = FileSystemInspectorLogic.analyzeFile(meta, largeFileThresholdBytes = 100L * 1024L * 1024L, recentMs = 30L * 24 * 60 * 60 * 1000L)
        assertTrue(findings.any { it.title.contains("Large file detected") })
    }

    @Test
    fun `executable extension is detected`() {
        val meta = FileMetadata(
            uri = null,
            name = "script.sh",
            path = "/storage/emulated/0/Download/script.sh",
            size = 2048L,
            lastModified = System.currentTimeMillis() - 1000L,
            mimeType = "text/x-shellscript",
            sha256 = null
        )
        val findings = FileSystemInspectorLogic.analyzeFile(meta, largeFileThresholdBytes = 100L * 1024L * 1024L, recentMs = 30L * 24 * 60 * 60 * 1000L)
        assertTrue(findings.any { it.title.contains("executable-like") })
    }
}
