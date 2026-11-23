package dev.agentdroid.app

import android.content.DialogInterface
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

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
    fun logSurvivesActivityRestart() {
        val first = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        first.findViewById<Button>(R.id.run).performClick()

        val second = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        val restored = second.findViewById<TextView>(R.id.log).text.toString()
        assertTrue(
            "expected the earlier session in the restored trail, got: $restored",
            restored.contains("Planning task") && restored.contains("Session started"),
        )
    }

    @Test
    fun captureButtonReportsWhenScreenAccessIsOff() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        activity.findViewById<Button>(R.id.capture).performClick()

        val log = activity.findViewById<TextView>(R.id.log).text.toString()
        assertTrue(
            "expected the screen-access hint, got: $log",
            log.contains(activity.getString(R.string.screen_capture_off)),
        )
    }

    @Test
    fun resetAsksBeforeWipingTheTrail() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.findViewById<Button>(R.id.run).performClick()
        val log = activity.findViewById<TextView>(R.id.log)

        activity.findViewById<Button>(R.id.reset).performClick()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull("expected a confirmation dialog", dialog)
        assertTrue("trail must survive the tap alone", log.text.toString().contains("Planning task"))

        dialog!!.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("", log.text.toString())
    }
}
