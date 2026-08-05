package com.rynekryz.rynotes

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BACKUP_ENTRY_NAME = "rynotes_backup.json"

/**
 * Writes/reads a single-entry zip backup at a SAF content:// URI.
 * Kept as a plain object (not part of NoteViewModel) since it only does file I/O, no state.
 */
object BackupManager {

    fun writeBackupZip(context: Context, uri: Uri, json: String) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry(BACKUP_ENTRY_NAME))
                zip.write(json.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        } ?: throw IllegalStateException("Could not open output stream for backup file")
    }

    fun readBackupZip(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name == BACKUP_ENTRY_NAME || entry.name.endsWith(".json")) {
                        val reader = BufferedReader(InputStreamReader(zip, Charsets.UTF_8))
                        return reader.readText()
                    }
                    entry = zip.nextEntry
                }
            }
        } ?: throw IllegalStateException("Could not open input stream for backup file")
        throw IllegalStateException("No backup data found inside the zip file")
    }

    fun defaultFileName(): String {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd_HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "RyNotes_Backup_$timestamp.zip"
    }
}
