package dev.agentdroid.app.screen

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * The single capability ADR 0001 commits to: read the current screen.
 *
 * The service performs no actions, opens no network connections and stores
 * nothing. It can only run while the user has switched it on under
 * Settings → Accessibility, and everything it exposes goes through
 * [ScreenAccess.currentScreen] as a [ScreenSnapshot].
 */
class ScreenAccessService : AccessibilityService() {

    /**
     * Nothing to do per event yet: screens are read on demand. Once an agent
     * loop exists this is where a "screen changed" wake-up would live.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = this
    }

    override fun onDestroy() {
        connected = null
        super.onDestroy()
    }

    internal fun readCurrentScreen(): ScreenSnapshot? = snapshotOf(rootInActiveWindow)

    companion object {
        private var connected: ScreenAccessService? = null

        internal fun instance(): ScreenAccessService? = connected
    }
}

/**
 * Narrow entry point for the rest of the app. Reading only: there is no way to
 * tap, type or navigate from here by design.
 */
object ScreenAccess {

    /** True when the user has enabled this service in the OS accessibility settings. */
    fun isEnabled(context: Context): Boolean {
        val resolver = context.contentResolver
        if (Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) {
            return false
        }
        val enabled = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()

        // Settings stores either the full or the short component name.
        val component = ComponentName(context, ScreenAccessService::class.java)
        return enabled.split(':').any {
            it.equals(component.flattenToString(), ignoreCase = true) ||
                it.equals(component.flattenToShortString(), ignoreCase = true)
        }
    }

    /**
     * The screen as it is right now, or null when the service is off or its
     * active window cannot be read. Callers check [isEnabled] first to tell
     * "off" apart from "nothing readable".
     */
    fun currentScreen(): ScreenSnapshot? = ScreenAccessService.instance()?.readCurrentScreen()

    /** Opens the OS accessibility settings, where the user grants or revokes access. */
    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
