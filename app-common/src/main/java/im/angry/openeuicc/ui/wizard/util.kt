package im.angry.openeuicc.ui.wizard

import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.service.EuiccChannelManagerService
import net.typeblog.lpac_jni.ProfileDownloadInput

/**
 * Launch a profile download through EuiccChannelManagerService on behalf of a UI
 * component, and auto-confirm the metadata-confirmation step.
 *
 * This is the single implementation of the UI ↔ service contract for profile
 * download, factored out of the fragment so that it can be exercised by unit
 * tests on a plain JVM:
 *
 *  1. Wait for any in-flight foreground task to finish (the service runs one at a time);
 *  2. Resolve the physical slot/port pair for the requested logical slot via [manager];
 *  3. Launch the download through [service];
 *  4. Send `true` on the handle's back channel so the task does not stall at
 *     ProfileDownloadState.ConfirmingDownload (see
 *     EuiccChannelManagerService.launchProfileDownloadTask). Without this, the
 *     download would wait for a minute and then be cancelled.
 *
 * The back channel is buffered (see launchForegroundTask), so the send in step 4
 * does not block: this function returns immediately and the caller can subscribe
 * to the task's state flow before the download has even started, which is what
 * lets the UI render every intermediate progress state.
 *
 * IMPORTANT: DownloadWizardProgressFragment MUST call this function rather than
 * re-implementing the steps above inline -- the tests only protect code that
 * actually runs in production. See DownloadTaskLauncherTest.
 */
suspend fun launchProfileDownload(
    service: EuiccChannelManagerService,
    manager: EuiccChannelManager,
    logicalSlotId: Int,
    seId: EuiccChannel.SecureElementId,
    input: ProfileDownloadInput,
    autoConfirm: Boolean = true
): EuiccChannelManagerService.ForegroundTaskHandle {
    service.waitForForegroundTask()

    val (slotId, portId) = manager.withEuiccChannel(logicalSlotId, seId) { channel ->
        Pair(channel.slotId, channel.portId)
    }

    val handle = service.launchProfileDownloadTask(slotId, portId, seId, input)

    if (autoConfirm) {
        // TODO: Ask user to confirm metadata for real if we need it
        handle.backChannel.send(true)
    }

    return handle
}
