package im.angry.openeuicc.service

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests the confirmation contract between EuiccChannelManagerService and its
 * subscribers:
 *
 *  - the subscriber MUST send a Boolean to the task's back channel when the task
 *    reaches a confirmation point;
 *  - `true` continues the task, `false` cancels it;
 *  - if no signal arrives within the timeout, the task defaults to cancelling.
 *
 * This is exactly the logic that used to live inline in
 * EuiccChannelManagerService.launchProfileDownloadTask and regressed during the
 * "merge back-communication channels" refactor (the UI stopped sending the
 * confirmation, so every download stalled for a minute and then cancelled).
 *
 * If you change awaitBackChannelConfirmation(), the download task and its UI
 * callers (DownloadWizardProgressFragment / launchProfileDownload) must change
 * with it.
 */
class ForegroundTaskConfirmationTest {

    @Test
    fun `confirmation blocks the task until a true signal arrives`() {
        val backChannel = Channel<Any>()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val result = executor.submit<Boolean> {
                awaitBackChannelConfirmation(backChannel, timeoutMillis = 10_000)
            }

            // The back channel is rendezvous: send() suspends until the receiver is
            // ready, so this also proves the task really was blocked on the channel.
            runBlocking { backChannel.send(true) }

            assertTrue(
                "task should have continued after receiving true",
                result.get(2, TimeUnit.SECONDS)
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `a false signal cancels the task`() {
        val backChannel = Channel<Any>()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val result = executor.submit<Boolean> {
                awaitBackChannelConfirmation(backChannel, timeoutMillis = 10_000)
            }

            runBlocking { backChannel.send(false) }

            assertEquals(false, result.get(2, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `no signal within the timeout defaults to cancelling`() {
        val backChannel = Channel<Any>()
        val start = System.currentTimeMillis()

        val result = awaitBackChannelConfirmation(backChannel, timeoutMillis = 100)

        val elapsed = System.currentTimeMillis() - start
        assertEquals(false, result)
        assertTrue(
            "timeout should not take anywhere near the default 60s, took ${elapsed}ms",
            elapsed < 5_000
        )
    }

    @Test
    fun `a non-boolean payload counts as a contract violation and surfaces as an error`() {
        val backChannel = Channel<Any>()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val result = executor.submit<Boolean> {
                awaitBackChannelConfirmation(backChannel, timeoutMillis = 10_000)
            }

            runBlocking { backChannel.send("oops") }

            // A non-Boolean value is not a valid confirmation; the ClassCastException
            // propagates to the caller (the foreground task machinery turns it into
            // a task failure), rather than being silently treated as cancel.
            assertTrue(
                "non-Boolean payload should surface as an exception",
                runCatching { result.get(2, TimeUnit.SECONDS) }
                    .exceptionOrNull()?.cause is ClassCastException
            )
        } finally {
            executor.shutdownNow()
        }
    }
}
