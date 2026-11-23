package dev.agentdroid.app

import android.app.Activity
import android.os.Bundle
import android.text.format.DateFormat as AndroidDateFormat
import android.widget.Button
import android.widget.TextView
import dev.agentdroid.app.audit.AuditEntry
import dev.agentdroid.app.audit.AuditTrail
import dev.agentdroid.app.screen.ScreenAccess
import dev.agentdroid.app.screen.ScreenNode
import java.text.DateFormat
import java.util.Date

/**
 * Minimal agent console: shows an activity log and lets you start/stop a
 * local placeholder agent run. Replace [runAgent] with the real agent loop.
 *
 * The log is the on-device audit trail from ADR 0001: every line is appended to
 * [AuditTrail] and replayed on start, so it survives a restart. Reset wipes it.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var screenAccess: TextView

    private lateinit var trail: AuditTrail
    private val entries = mutableListOf<AuditEntry>()
    private lateinit var clock: DateFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        log = findViewById(R.id.log)
        screenAccess = findViewById(R.id.screen_access)
        screenAccess.setOnClickListener { ScreenAccess.openSettings(this) }

        clock = AndroidDateFormat.getTimeFormat(this)
        trail = AuditTrail(this)
        entries += trail.readAll()

        findViewById<Button>(R.id.run).setOnClickListener { runAgent() }
        findViewById<Button>(R.id.capture).setOnClickListener { captureScreen() }
        findViewById<Button>(R.id.reset).setOnClickListener {
            entries.clear()
            trail.clear()
            render()
        }

        append("Session started")
    }

    // The OS accessibility toggle can change while this screen is in the
    // background, so the label is refreshed every time it comes back.
    override fun onResume() {
        super.onResume()
        screenAccess.setText(
            if (ScreenAccess.isEnabled(this)) R.string.screen_access_on else R.string.screen_access_off,
        )
    }

    private fun runAgent() {
        status.setText(R.string.status_running)
        append("Planning task…")
        append("No tools configured yet")
        append("Idle")
        status.setText(R.string.status_idle)
    }

    /**
     * Read-only capture through the accessibility service: records what is on
     * screen and nothing more. No actions, no network — this exists to exercise
     * the typed surface the agent will later reason over.
     */
    private fun captureScreen() {
        if (!ScreenAccess.isEnabled(this)) {
            append(getString(R.string.screen_capture_off))
            return
        }
        val snapshot = ScreenAccess.currentScreen()
        if (snapshot == null) {
            append(getString(R.string.screen_capture_unreadable))
            return
        }

        val capture = buildList {
            add(
                getString(
                    R.string.screen_capture_summary,
                    snapshot.packageName ?: getString(R.string.screen_package_unknown),
                    snapshot.nodes.size,
                    if (snapshot.truncated) getString(R.string.screen_capture_truncated) else "",
                ),
            )
            snapshot.nodes
                .mapNotNull { it.describe() }
                .distinct()
                .take(MAX_CAPTURE_LINES)
                .forEach { add("  $it") }
        }
        append(capture.joinToString("\n"))
    }

    /** One log line for a labelled node; containers without a label are skipped. */
    private fun ScreenNode.describe(): String? {
        val label = text?.trim()?.takeIf { it.isNotEmpty() }
            ?: contentDescription?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val kind = className?.substringAfterLast('.') ?: "node"
        val tappable = if (clickable) " tappable" else ""
        return "[$kind$tappable] ${label.take(60)}"
    }

    private fun append(text: String) {
        val entry = AuditEntry(System.currentTimeMillis(), text)
        entries += entry
        trail.record(entry)
        render()
    }

    /** The trail is only ever displayed, never edited in place. */
    private fun render() {
        log.text = entries.joinToString("\n") { entry ->
            "> ${clock.format(Date(entry.atMillis))} ${entry.text}"
        }
    }

    private companion object {
        const val MAX_CAPTURE_LINES = 5
    }
}
