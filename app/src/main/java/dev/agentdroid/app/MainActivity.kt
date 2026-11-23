package dev.agentdroid.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import dev.agentdroid.app.screen.ScreenAccess
import dev.agentdroid.app.screen.ScreenNode

/**
 * Minimal agent console: shows an activity log and lets you start/stop a
 * local placeholder agent run. Replace [runAgent] with the real agent loop.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var screenAccess: TextView

    private val lines = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        log = findViewById(R.id.log)
        screenAccess = findViewById(R.id.screen_access)
        screenAccess.setOnClickListener { ScreenAccess.openSettings(this) }

        findViewById<Button>(R.id.run).setOnClickListener { runAgent() }
        findViewById<Button>(R.id.capture).setOnClickListener { captureScreen() }
        findViewById<Button>(R.id.reset).setOnClickListener {
            lines.clear()
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
     * Read-only capture through the accessibility service: prints what is on
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

        append(
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
            .forEach { append("  $it") }
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

    private fun append(line: String) {
        lines += line
        render()
    }

    private fun render() {
        log.text = lines.joinToString("\n") { "> $it" }
    }

    private companion object {
        const val MAX_CAPTURE_LINES = 5
    }
}
