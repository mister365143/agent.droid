package dev.agentdroid.app.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditTrailTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun entriesSurviveANewInstanceAsOneLineEach() {
        AuditTrail(context).apply {
            clear()
            record(AuditEntry(1_000L, "Session started"))
        }
        val capture = AuditEntry(2_000L, "Screen a.b · 3 nodes\n\t[Button] \"Go\" C:\\path")
        AuditTrail(context).record(capture)

        assertEquals(2, File(context.filesDir, "audit.log").readLines().size)
        assertEquals(
            listOf(AuditEntry(1_000L, "Session started"), capture),
            AuditTrail(context).readAll(),
        )
    }

    @Test
    fun clearRemovesTheStoredTrail() {
        val trail = AuditTrail(context)
        trail.clear()
        trail.record(AuditEntry(1L, "gone"))

        trail.clear()

        assertTrue(trail.readAll().isEmpty())
    }

    @Test
    fun unreadableLinesAreSkipped() {
        val trail = AuditTrail(context)
        trail.clear()
        trail.record(AuditEntry(1L, "good"))
        File(context.filesDir, "audit.log").appendText("garbage\n{\"at\":2}\n")

        assertEquals(listOf("good"), trail.readAll().map { it.text })
    }
}
