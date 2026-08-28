package im.angry.openeuicc.ui.wizard

import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import im.angry.openeuicc.common.R
import im.angry.openeuicc.service.EuiccChannelManagerService
import im.angry.openeuicc.testutil.FakeEuiccChannel
import im.angry.openeuicc.testutil.FakeEuiccChannelManager
import im.angry.openeuicc.testutil.FakeLpa
import im.angry.openeuicc.testutil.TestOpenEuiccApplication
import im.angry.openeuicc.testutil.awaitMainLooper
import kotlinx.coroutines.runBlocking
import net.typeblog.lpac_jni.ProfileDownloadInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A Robolectric UI test that drives the REAL DownloadWizardActivity through the
 * whole wizard flow -- slot select -> method select -> details -> download
 * progress -- until the download completes.
 *
 * Only the dependencies *below* EuiccChannelManagerService are faked (manager ->
 * channel -> LPA, see testutil.Fakes). The activity, every wizard fragment, the
 * in-process service binding and the entire foreground-task machinery are the
 * real production code, so this test protects the actual UI flow that a user
 * experiences, not just the launchProfileDownload() contract in isolation.
 *
 * The FakeLpa simulates a download that reaches ProfileDownloadState.ConfirmingDownload
 * and blocks until the subscriber confirms through the task's back channel --
 * exactly what a real download does before finishing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestOpenEuiccApplication::class)
class DownloadWizardFlowTest {

    private val input = ProfileDownloadInput(
        address = "smdp.example.com",
        matchingId = "matching-id",
        imei = "imei",
        confirmationCode = "conf-code"
    )

    private lateinit var activity: DownloadWizardActivity
    private lateinit var service: EuiccChannelManagerService
    private lateinit var channel: FakeEuiccChannel
    private lateinit var manager: FakeEuiccChannelManager

    @Before
    fun setUp() {
        channel = FakeEuiccChannel(slotId = 1, portId = 2)
        manager = FakeEuiccChannelManager(channel)
        TestOpenEuiccApplication.fakeEuiccChannelManager = manager

        // Robolectric does not auto-create services on bindService(); register the
        // REAL EuiccChannelManagerService (with the fake manager below it) as the
        // binding target so that BaseEuiccAccessActivity connects to it in-process.
        service = Robolectric.buildService(EuiccChannelManagerService::class.java).get()
        val bindIntent = Intent(
            RuntimeEnvironment.getApplication(),
            EuiccChannelManagerService::class.java
        )
        shadowOf(RuntimeEnvironment.getApplication())
            .setComponentNameAndServiceForBindServiceForIntent(
                bindIntent,
                ComponentName(
                    RuntimeEnvironment.getApplication(),
                    EuiccChannelManagerService::class.java
                ),
                service.onBind(Intent())
            )

        val intent = DownloadWizardActivity.newIntent(
            RuntimeEnvironment.getApplication(),
            logicalSlotId = 0
        )
        val controller = Robolectric.buildActivity(DownloadWizardActivity::class.java, intent)
        // The activity does not declare a theme itself; it inherits the host
        // app's application-level theme (Theme.OpenEUICC), which Robolectric
        // does not merge in for a library module. AppCompatActivity requires an
        // AppCompat theme, so apply it before onCreate() runs.
        controller.get().setTheme(R.style.Theme_OpenEUICC)
        activity = controller.setup().get()
        idle()

        // Wait until the in-process service connection is up and onInit() has run.
        runBlocking {
            awaitMainLooper { activity.euiccChannelManagerLoaded.isCompleted }
        }
        idle()
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun currentFragment(): DownloadWizardActivity.DownloadWizardStepFragment? =
        activity.supportFragmentManager
            .findFragmentById(R.id.step_fragment_container)
            as? DownloadWizardActivity.DownloadWizardStepFragment

    /**
     * RecyclerView children are only laid out when the Robolectric main looper
     * runs the view traversal; if that hasn't happened yet, force a layout so
     * that findViewHolderForAdapterPosition() works.
     */
    private fun RecyclerView.ensureLaidOut() {
        idle()
        if (childCount == 0) {
            measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, 1080, 1920)
            idle()
        }
    }

    private fun clickListItem(recyclerView: RecyclerView, position: Int) {
        recyclerView.ensureLaidOut()
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        assertNotNull("list item $position should be visible", holder)
        holder!!.itemView.performClick()
        idle()
    }

    private fun clickNext() {
        activity.findViewById<Button>(R.id.download_wizard_next).performClick()
        idle()
    }

    @Test
    fun `wizard runs the full flow through the real service to a completed download`() = runBlocking {
        val lpa = channel.lpa as FakeLpa

        // ---- Slot select: the fake manager exposes exactly one eUICC ----
        val slotList = activity.findViewById<RecyclerView>(R.id.download_slot_list)
        awaitMainLooper { (slotList.adapter?.itemCount ?: 0) > 0 }
        assertEquals(1, slotList.adapter?.itemCount)
        clickListItem(slotList, 0)

        // ---- Next -> method select ----
        clickNext()
        assertTrue(currentFragment() is DownloadWizardMethodSelectFragment)

        // ---- Pick the manual entry -> details form ----
        val methodList = activity.findViewById<RecyclerView>(R.id.download_method_list)
        awaitMainLooper { (methodList.adapter?.itemCount ?: 0) == 4 }
        clickListItem(methodList, 3) // download_wizard_method_manual
        assertTrue(currentFragment() is DownloadWizardDetailsFragment)

        // ---- Fill the form exactly as the user would ----
        val fields = mapOf(
            R.id.profile_download_server to input.address,
            R.id.profile_download_code to input.matchingId!!,
            R.id.profile_download_confirmation_code to input.confirmationCode!!,
            R.id.profile_download_imei to input.imei!!
        )
        fields.forEach { (id, text) ->
            activity.findViewById<TextInputLayout>(id).editText!!.setText(text)
        }
        idle()

        // ---- Next -> download progress; the download launches via the REAL service ----
        clickNext()
        assertTrue(currentFragment() is DownloadWizardProgressFragment)

        // The service self-starts via startForegroundService(); Robolectric does
        // not deliver that to onStartCommand() on its own (see
        // DownloadTaskLauncherTest), so simulate the system call once the task
        // has had a chance to subscribe to foregroundStarted. The fake manager
        // having resolved the logical slot proves the launch has been reached.
        awaitMainLooper { manager.logicalChannelRequests.isNotEmpty() }
        idle()
        service.onStartCommand(Intent(), 0, 1)
        idle()

        // ---- The download must complete and the UI must reflect it ----
        awaitMainLooper { lpa.downloadReturned.isCompleted }
        awaitMainLooper { currentFragment()?.hasNext == true }

        // The LPA received the exact input the UI passed, and the metadata
        // confirmation was auto-approved through the back channel.
        assertEquals(input, lpa.downloadInput)
        assertEquals(true, lpa.awaitDownloadResult())

        // The service resolved the physical slot/port through the manager and
        // launched the download with them.
        assertEquals(1 to 2, manager.physicalChannelRequests.first())

        // UI: every step the download reached shows a checkmark; nothing is stuck
        // in progress; the wizard offers to continue (i.e. no error).
        val progressList = activity.findViewById<RecyclerView>(R.id.download_progress_list)
        progressList.ensureLaidOut()
        for (i in 0..2) {
            val holder = progressList.findViewHolderForAdapterPosition(i)
            assertNotNull("progress item $i should be visible", holder)
            val icon = holder!!.itemView.findViewById<ImageView>(R.id.download_progress_icon)
            assertTrue("step $i should be marked done", icon.visibility == View.VISIBLE)
        }
        // hasNext is true and no error -> the Next button must be visible so the
        // user can leave the (successful) download screen.
        assertEquals(
            View.VISIBLE,
            activity.findViewById<Button>(R.id.download_wizard_next).visibility
        )

        // Clicking Next after a successful download must finish the wizard
        // (createNextFragment() returns null when there is no error).
        clickNext()
        assertTrue("wizard should finish after a successful download", activity.isFinishing)
    }
}
