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
    fun entriesSurviveANewInstance() {
        AuditTrail(context).apply {
            clear()
            record(AuditEntry(1_000L, AuditEntry.Kind.LOG, "Session started"))
        }
        AuditTrail(context).record(
            AuditEntry(2_000L, AuditEntry.Kind.CAPTURE, "Screen a.b · 3 nodes\n\t[Button] Go"),
        )

        val reopened = AuditTrail(context).recent()

        assertEquals(2, reopened.size)
        assertEquals("Session started", reopened[0].text)
        assertEquals(AuditEntry.Kind.LOG, reopened[0].kind)
        assertEquals(1_000L, reopened[0].atMillis)
        assertEquals(AuditEntry.Kind.CAPTURE, reopened[1].kind)
        assertEquals(2_000L, reopened[1].atMillis)
        assertEquals("Screen a.b · 3 nodes\n\t[Button] Go", reopened[1].text)
    }

    @Test
    fun recentKeepsOnlyTheNewestEntries() {
        val trail = AuditTrail(context)
        trail.clear()
        (1..5).forEach { trail.record(AuditEntry(it.toLong(), AuditEntry.Kind.LOG, "line $it")) }

        assertEquals(listOf("line 4", "line 5"), trail.recent(limit = 2).map { it.text })
    }

    @Test
    fun clearRemovesTheStoredTrail() {
        val trail = AuditTrail(context)
        trail.clear()
        trail.record(AuditEntry(1L, AuditEntry.Kind.LOG, "gone"))

        trail.clear()

        assertTrue(trail.recent().isEmpty())
    }

    @Test
    fun malformedLinesAreSkipped() {
        val trail = AuditTrail(context)
        trail.clear()
        trail.record(AuditEntry(1L, AuditEntry.Kind.LOG, "good"))
        File(context.filesDir, "audit.log").appendText("garbage\n2\tNOT_A_KIND\tbad\n")

        assertEquals(listOf("good"), trail.recent().map { it.text })
    }
}
