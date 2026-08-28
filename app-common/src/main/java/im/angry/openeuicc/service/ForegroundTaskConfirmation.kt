package im.angry.openeuicc.service

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Block the calling thread until a foreground task subscriber confirms or cancels
 * the task through its back channel.
 *
 * This encodes the confirmation contract between EuiccChannelManagerService and
 * its subscribers:
 *  - the subscriber MUST send a Boolean to the back channel once the task reaches a
 *    confirmation point (e.g. ProfileDownloadState.ConfirmingDownload);
 *  - `true` continues the task, `false` cancels it;
 *  - if no signal arrives within [timeoutMillis], the task defaults to cancelling.
 *
 * This is a blocking function because it is invoked from synchronous (non-suspend)
 * callbacks (e.g. ProfileDownloadCallback) that run on a background thread.
 *
 * This file intentionally has no Android dependencies so it can be unit-tested
 * on a plain JVM. See ForegroundTaskConfirmationTest; if you change the contract
 * here, the download task in EuiccChannelManagerService and its UI callers must
 * change with it.
 */
fun awaitBackChannelConfirmation(
    backChannel: Channel<Any>,
    timeoutMillis: Long = 60_000
): Boolean = runBlocking {
    try {
        withTimeout(timeoutMillis) {
            backChannel.receive() as Boolean
        }
    } catch (_: TimeoutCancellationException) {
        false
    }
}
