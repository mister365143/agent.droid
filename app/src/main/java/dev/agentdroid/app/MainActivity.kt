package dev.agentdroid.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/**
 * Minimal agent console: shows an activity log and lets you start/stop a
 * local placeholder agent run. Replace [runAgent] with the real agent loop.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var log: TextView

    private val lines = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        log = findViewById(R.id.log)

        findViewById<Button>(R.id.run).setOnClickListener { runAgent() }
        findViewById<Button>(R.id.reset).setOnClickListener {
            lines.clear()
            render()
        }

        append("Session started")
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
