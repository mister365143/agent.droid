package dev.agentdroid.app.audit

import android.content.Context
import java.io.File

/** One line of the local audit trail. */
data class AuditEntry(val atMillis: Long, val kind: Kind, val text: String) {
    enum class Kind { LOG, CAPTURE }
}

/**
 * Append-only audit trail on the device (ADR 0001): one escaped, tab-separated
 * line per record in the app's private files directory. No network, no
 * database — it can be inspected with
 * `adb shell run-as dev.agentdroid.app cat files/audit.log`.
 *
 * [clear] exists because the console's Reset button wipes the trail; nothing
 * else ever rewrites or truncates it.
 */
class AuditTrail(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    /** Appends [entry]. A failed write must not take the console down. */
    fun record(entry: AuditEntry) {
        runCatching {
            file.appendText("${entry.atMillis}\t${entry.kind.name}\t${escape(entry.text)}\n")
        }
    }

    /** The newest [limit] entries, oldest first; empty when nothing is stored. */
    fun recent(limit: Int = DEFAULT_LIMIT): List<AuditEntry> {
        if (!file.isFile) return emptyList()
        val lines = runCatching { file.readLines() }.getOrElse { return emptyList() }
        return lines.mapNotNull { parse(it) }.takeLast(limit)
    }

    fun clear() {
        file.delete()
    }

    private fun parse(line: String): AuditEntry? {
        val parts = line.split('\t', limit = 3)
        if (parts.size != 3) return null
        val atMillis = parts[0].toLongOrNull() ?: return null
        val kind = runCatching { AuditEntry.Kind.valueOf(parts[1]) }.getOrNull() ?: return null
        return AuditEntry(atMillis, kind, unescape(parts[2]))
    }

    private companion object {
        const val FILE_NAME = "audit.log"
        const val DEFAULT_LIMIT = 200

        // Tabs and newlines separate records, so they must not appear raw.
        fun escape(text: String): String = text
            .replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        fun unescape(text: String): String {
            val out = StringBuilder(text.length)
            var index = 0
            while (index < text.length) {
                val char = text[index]
                val next = if (char == '\\' && index + 1 < text.length) text[index + 1] else null
                when (next) {
                    't' -> { out.append('\t'); index += 2 }
                    'n' -> { out.append('\n'); index += 2 }
                    'r' -> { out.append('\r'); index += 2 }
                    '\\' -> { out.append('\\'); index += 2 }
                    else -> { out.append(char); index++ }
                }
            }
            return out.toString()
        }
    }
}
