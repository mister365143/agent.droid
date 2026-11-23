package dev.agentdroid.app.screen

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

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
        current = WeakReference(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        current = null
        return super.onUnbind(intent)
    }

    /** Reads the active window now, or null if it cannot be read. */
    internal fun readCurrentScreen(): ScreenSnapshot? {
        val root = try {
            rootInActiveWindow
        } catch (_: SecurityException) {
            // The user can revoke accessibility access at any time.
            null
        }
        return snapshotOf(root)
    }

    companion object {
        private var current: WeakReference<ScreenAccessService>? = null

        internal fun instance(): ScreenAccessService? = current?.get()
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
        ) ?: return false

        val expected = ComponentName(context, ScreenAccessService::class.java)
        return enabled.split(':')
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it.matches(expected) }
    }

    /**
     * The screen as it is right now, or null when the service is off or its
     * active window cannot be read. Callers must check [isEnabled] first to
     * tell "off" apart from "nothing readable".
     */
    fun currentScreen(): ScreenSnapshot? = ScreenAccessService.instance()?.readCurrentScreen()

    /** Opens the OS accessibility settings, where the user grants or revokes access. */
    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Settings may store the component with either a full or a relative class name. */
    private fun ComponentName.matches(expected: ComponentName): Boolean =
        packageName == expected.packageName &&
            (className == expected.className ||
                expected.className.endsWith(className.removePrefix(".")))
}
