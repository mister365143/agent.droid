package dev.agentdroid.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import dev.agentdroid.app.screen.ScreenAccess

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

    private fun append(line: String) {
        lines += line
        render()
    }

    private fun render() {
        log.text = lines.joinToString("\n") { "> $it" }
    }
}
