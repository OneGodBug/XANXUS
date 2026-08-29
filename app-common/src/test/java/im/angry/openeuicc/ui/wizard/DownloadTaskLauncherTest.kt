package im.angry.openeuicc.ui.wizard

import android.content.Intent
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.service.EuiccChannelManagerService
import im.angry.openeuicc.testutil.FakeEuiccChannel
import im.angry.openeuicc.testutil.FakeEuiccChannelManager
import im.angry.openeuicc.testutil.FakeLpa
import im.angry.openeuicc.testutil.TestOpenEuiccApplication
import im.angry.openeuicc.testutil.awaitMainLooper
import android.os.Looper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.robolectric.Shadows.shadowOf
import net.typeblog.lpac_jni.ProfileDownloadInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests launchProfileDownload(), the freestanding implementation of the
 * UI → service contract for profile download, against the REAL
 * EuiccChannelManagerService (built with Robolectric). Only the manager /
 * channel / LPA below the service are faked (see testutil.Fakes).
 *
 * DownloadWizardProgressFragment MUST call launchProfileDownload() and not
 * re-implement the launch + auto-confirmation inline; the tests here only
 * protect code that actually runs in production. If you change the behavior
 * verified in this file, update the fragment at the same time.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestOpenEuiccApplication::class)
class DownloadTaskLauncherTest {

    private val seId = EuiccChannel.SecureElementId.DEFAULT
    private val input = ProfileDownloadInput(
        address = "smdp.example.com",
        matchingId = "matching-id",
        imei = "imei",
        confirmationCode = "conf-code"
    )

    private lateinit var service: EuiccChannelManagerService
    private lateinit var channel: FakeEuiccChannel
    private lateinit var manager: FakeEuiccChannelManager

    @Before
    fun setUp() {
        channel = FakeEuiccChannel(slotId = 1, portId = 2)
        manager = FakeEuiccChannelManager(channel)
        TestOpenEuiccApplication.fakeEuiccChannelManager = manager
        service = Robolectric.buildService(EuiccChannelManagerService::class.java).get()
    }

    /**
     * The service self-starts via startForegroundService(); Robolectric does not
     * deliver that to onStartCommand() on its own, so simulate the system call.
     * Must be invoked after the launch coroutine has had a chance to subscribe to
     * foregroundStarted (otherwise the emission is dropped -- foregroundStarted
     * has no replay). In production the system delivers the start asynchronously,
     * so the subscriber is always ready by then.
     *
     * When launchProfileDownload is running inside an `async` coroutine, we must
     * first yield() so it can call launchProfileDownloadTask() and park at
     * backChannel.send(true); only then does the service's launch coroutine exist
     * and subscribe to foregroundStarted.
     */
    private suspend fun startService() {
        yield()
        shadowOf(Looper.getMainLooper()).idle()
        service.onStartCommand(Intent(), 0, 1)
    }

    private suspend fun awaitTaskDone(
        handle: EuiccChannelManagerService.ForegroundTaskHandle
    ): EuiccChannelManagerService.ForegroundTaskState.Done = coroutineScope {
        // Collect on the test thread (inherited from runBlocking): flow emissions always
        // run the collect body on this coroutine's dispatcher, so adds happen on the same
        // thread that awaitMainLooper reads from -- no concurrent access, plain list is safe.
        val states = mutableListOf<EuiccChannelManagerService.ForegroundTaskState>()
        val collector = async { handle.stateFlow.collect { states += it } }
        try {
            awaitMainLooper {
                states.any { it is EuiccChannelManagerService.ForegroundTaskState.Done }
            }
        } finally {
            collector.cancel()
        }
        states.last() as EuiccChannelManagerService.ForegroundTaskState.Done
    }

    @Test
    fun `launchProfileDownload launches through the real service and auto-confirms`() = runBlocking {
        val lpa = channel.lpa as FakeLpa

        // launchProfileDownload blocks until the service consumes the auto-confirmation
        // (rendezvous back channel), so pump the main looper until it returns.
        val handle = withTimeout(10_000) {
            val deferred = async { launchProfileDownload(service, manager, logicalSlotId = 0, seId, input) }
            // launchProfileDownload is now suspended at backChannel.send(true); deliver
            // the start command so the service machinery can run.
            startService()
            awaitMainLooper { deferred.isCompleted }
            deferred.await()
        }

        // The service must have been asked to download with the physical slot/port
        // resolved through the manager (1, 2) and the exact input the UI passed.
        assertEquals(1 to 2, manager.physicalChannelRequests.first())
        assertEquals(input, lpa.downloadInput)

        // The critical part that regressed once: the UI MUST send a confirmation on
        // the back channel; otherwise the download stalls and then cancels. Here the
        // LPA's callback must have seen `true`.
        assertEquals(true, lpa.awaitDownloadResult())

        // The task must have completed with no error.
        val done = awaitTaskDone(handle)
        assertNull(done.error)
    }

    @Test
    fun `autoConfirm=false leaves confirmation to the UI`() = runBlocking {
        val lpa = channel.lpa as FakeLpa

        val handle = launchProfileDownload(
            service, manager, logicalSlotId = 0, seId, input, autoConfirm = false
        )
        startService()

        // The download must reach the LPA and stall at the confirmation step...
        awaitMainLooper { lpa.downloadStarted.isCompleted }
        assertFalse(
            "download must not complete before a manual confirmation",
            lpa.downloadReturned.isCompleted
        )

        // ...until the UI confirms through the back channel itself.
        handle.backChannel.send(true)

        assertEquals(true, lpa.awaitDownloadResult())
    }
}
