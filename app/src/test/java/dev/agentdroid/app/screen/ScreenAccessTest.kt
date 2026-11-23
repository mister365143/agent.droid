package dev.agentdroid.app.screen

import android.graphics.Rect
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenAccessTest {

    @Test
    fun unreadableScreenYieldsNoSnapshot() {
        assertNull(snapshotOf(null))
    }

    @Suppress("DEPRECATION")
    @Test
    fun mapsNodeIntoReadOnlySnapshot() {
        val node = AccessibilityNodeInfo.obtain()
        node.text = "Open settings"
        node.className = "android.widget.Button"
        node.contentDescription = "settings"
        node.setBoundsInScreen(Rect(0, 10, 100, 40))

        val snapshot = snapshotOf(node, nowMillis = 1234L)

        assertNotNull(snapshot)
        assertEquals(1234L, snapshot!!.capturedAtMillis)
        assertFalse(snapshot.truncated)
        assertEquals(1, snapshot.nodes.size)
        val mapped = snapshot.nodes[0]
        assertEquals("Open settings", mapped.text)
        assertEquals("settings", mapped.contentDescription)
        assertEquals("android.widget.Button", mapped.className)
        assertEquals(Bounds(0, 10, 100, 40), mapped.bounds)
    }

    @Test
    fun reportsDisabledUntilTheServiceIsSwitchedOn() {
        val context = RuntimeEnvironment.getApplication()
        val resolver = context.contentResolver

        assertFalse(ScreenAccess.isEnabled(context))

        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        Settings.Secure.putString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "dev.agentdroid.app/.screen.ScreenAccessService",
        )
        assertTrue(ScreenAccess.isEnabled(context))

        Settings.Secure.putString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "com.example.other/.OtherService",
        )
        assertFalse(ScreenAccess.isEnabled(context))
    }
}
