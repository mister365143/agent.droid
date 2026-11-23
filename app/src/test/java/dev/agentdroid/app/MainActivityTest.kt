package dev.agentdroid.app

import android.widget.Button
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Start-up smoke test: builds the real activity, inflates the real layout and
 * runs onCreate. Catches the class of failure that would otherwise only appear
 * on a device — bad resources, a broken theme, a missing view, an NPE in
 * onCreate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    @Test
    fun startsIdleWithSessionLog() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        val status = activity.findViewById<TextView>(R.id.status).text.toString()
        assertEquals(activity.getString(R.string.status_idle), status)

        val log = activity.findViewById<TextView>(R.id.log).text.toString()
        assertTrue("expected session start line, got: $log", log.contains("Session started"))
    }

    @Test
    fun runAgentButtonAppendsToLog() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        activity.findViewById<Button>(R.id.run).performClick()

        val log = activity.findViewById<TextView>(R.id.log).text.toString()
        assertTrue("expected a planning line, got: $log", log.contains("Planning task"))
    }

    @Test
    fun resetButtonClearsLog() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        activity.findViewById<Button>(R.id.run).performClick()
        activity.findViewById<Button>(R.id.reset).performClick()

        assertEquals("", activity.findViewById<TextView>(R.id.log).text.toString())
    }
}
