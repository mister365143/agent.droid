package dev.agentdroid.app.audit

import android.content.Context
import org.json.JSONObject
import java.io.File

/** One record in the local audit trail. */
data class AuditEntry(val atMillis: Long, val text: String)

/**
 * Append-only audit trail on the device (ADR 0001): one JSON object per line in
 * the app's private files directory. No network, no database — it can be read
 * with `adb shell run-as dev.agentdroid.app cat files/audit.log`.
 *
 * [clear] exists because the console's Reset button wipes the trail; nothing
 * else ever rewrites it.
 */
class AuditTrail(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    /** Appends [entry]. Fails loudly: a silently missing audit record is worse. */
    fun record(entry: AuditEntry) {
        val line = JSONObject()
            .put("at", entry.atMillis)
            .put("text", entry.text)
        file.appendText("$line\n")
    }

    /** The whole stored trail, oldest first; empty when there is nothing yet. */
    fun readAll(): List<AuditEntry> {
        if (!file.isFile) return emptyList()
        return file.readLines().mapNotNull(::parse)
    }

    fun clear() {
        file.delete()
    }

    // A partial trailing line is possible if the app dies mid-write, and an
    // unreadable line must not stop the console from starting.
    private fun parse(line: String): AuditEntry? = runCatching {
        val json = JSONObject(line)
        AuditEntry(json.getLong("at"), json.getString("text"))
    }.getOrNull()

    private companion object {
        const val FILE_NAME = "audit.log"
    }
}
