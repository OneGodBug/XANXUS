package im.angry.openeuicc.testutil

import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.FakeUiccCardInfoCompat
import im.angry.openeuicc.util.FakeUiccPortInfoCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.EuiccInfo2
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.LocalProfileNotification
import net.typeblog.lpac_jni.ProfileDownloadCallback
import net.typeblog.lpac_jni.ProfileDownloadInput
import net.typeblog.lpac_jni.ProfileDownloadState

/**
 * Fakes for the dependencies *below* EuiccChannelManagerService (manager →
 * channel → LPA), used by Robolectric tests that exercise the real service.
 *
 * The service itself is never faked; tests build it with
 * Robolectric.buildService() and only swap out what the service talks to.
 */

/**
 * A LocalProfileAssistant whose downloadProfile() simulates a download that needs
 * metadata confirmation: it immediately reports
 * ProfileDownloadState.ConfirmingDownload to the callback and blocks until the
 * caller resolves the confirmation (which, in production, the service does by
 * waiting on the task's back channel). The callback's return value is recorded
 * so tests can assert whether the download was confirmed or cancelled.
 */
class FakeLpa : LocalProfileAssistant {
    /** Completes as soon as downloadProfile() is entered (task reached the LPA). */
    val downloadStarted = CompletableDeferred<Unit>()

    /** Completes with the callback result once downloadProfile() returns. */
    val downloadReturned = CompletableDeferred<Boolean>()

    /** The input passed to downloadProfile(). */
    var downloadInput: ProfileDownloadInput? = null

    override fun downloadProfile(input: ProfileDownloadInput, callback: ProfileDownloadCallback) {
        downloadInput = input
        downloadStarted.complete(Unit)
        // This blocks the caller (the service's IO thread) until the back channel
        // receives a Boolean -- exactly like a real download reaching the
        // ConfirmingDownload step.
        val result = callback.onStatusUpdate(ProfileDownloadState.ConfirmingDownload(null))
        downloadReturned.complete(result)
    }

    suspend fun awaitDownloadStarted() = downloadStarted.await()
    suspend fun awaitDownloadResult(): Boolean = downloadReturned.await()

    // ---- trivial implementations for the rest of the LPA surface ----
    override val valid = true
    override val profiles = emptyList<LocalProfileInfo>()
    override val notifications = emptyList<LocalProfileNotification>()
    override val eID = "fake-eid"
    override val euiccInfo2: EuiccInfo2? = null
    override fun setEs10xMss(mss: Byte) {}
    override fun enableProfile(iccid: String, refresh: Boolean): Boolean = true
    override fun disableProfile(iccid: String, refresh: Boolean): Boolean = true
    override fun deleteProfile(iccid: String): Boolean = true
    override fun deleteNotification(seqNumber: Long): Boolean = true
    override fun handleNotification(seqNumber: Long): Boolean = true
    override fun euiccMemoryReset() {}
    override fun setNickname(iccid: String, nickname: String) {}
    override fun close() {}
}

/**
 * A EuiccChannel that always "finds" the given LPA and physical slot/port.
 */
class FakeEuiccChannel(
    override val slotId: Int,
    override val portId: Int,
    override val lpa: LocalProfileAssistant = FakeLpa()
) : EuiccChannel {
    override val type = "fake"
    override val port = FakeUiccPortInfoCompat(FakeUiccCardInfoCompat(slotId))
    override val logicalSlotId = slotId
    override val seId = EuiccChannel.SecureElementId.DEFAULT
    override var hasMultipleSE = false
    override val valid = true
    override val atr: ByteArray? = null

    override val apduInterface = object : ApduInterface {
        override fun connect() {}
        override fun disconnect() {}
        override fun logicalChannelOpen(aid: ByteArray): Int = 1
        override fun logicalChannelClose(handle: Int) {}
        override fun transmit(handle: Int, tx: ByteArray): ByteArray = ByteArray(0)
        override val valid = true
    }

    override val isdrAid = ByteArray(0)
    override fun close() {}
}

/**
 * A EuiccChannelManager that hands out the same [channel] for every request and
 * records how it was asked to resolve channels (slot/port pairs).
 */
class FakeEuiccChannelManager(val channel: EuiccChannel) : EuiccChannelManager {
    /** Every withEuiccChannel(physical slot/port) request, in order. */
    val physicalChannelRequests = mutableListOf<Pair<Int, Int>>()

    /** Every withEuiccChannel(logical slot) request, in order. */
    val logicalChannelRequests = mutableListOf<Int>()

    override fun flowInternalEuiccPorts(): Flow<Pair<Int, Int>> =
        flowOf(Pair(channel.slotId, channel.portId))

    override fun flowAllOpenEuiccPorts(): Flow<Pair<Int, Int>> =
        flowOf(Pair(channel.slotId, channel.portId))

    override fun flowEuiccSecureElements(slotId: Int, portId: Int): Flow<EuiccChannel.SecureElementId> =
        flowOf(channel.seId)

    override suspend fun tryOpenUsbEuiccChannel(): Pair<android.hardware.usb.UsbDevice?, Boolean> =
        null to true

    override suspend fun waitForReconnect(physicalSlotId: Int, portId: Int, timeoutMillis: Long) {}

    override suspend fun findFirstAvailablePort(physicalSlotId: Int): Int = channel.portId

    override suspend fun findAvailablePorts(physicalSlotId: Int): List<Int> = listOf(channel.portId)

    override suspend fun <R> withEuiccChannel(
        physicalSlotId: Int,
        portId: Int,
        seId: EuiccChannel.SecureElementId,
        fn: suspend (EuiccChannel) -> R
    ): R {
        physicalChannelRequests += physicalSlotId to portId
        return fn(channel)
    }

    override suspend fun <R> withEuiccChannel(
        logicalSlotId: Int,
        seId: EuiccChannel.SecureElementId,
        fn: suspend (EuiccChannel) -> R
    ): R {
        logicalChannelRequests += logicalSlotId
        return fn(channel)
    }

    override fun invalidate() {}
}
