package net.typeblog.genericapi.telephony

import android.os.Build
import android.telephony.IccOpenLogicalChannelResponse.INVALID_CHANNEL
import android.telephony.IccOpenLogicalChannelResponse.STATUS_MISSING_RESOURCE
import android.telephony.IccOpenLogicalChannelResponse.STATUS_NO_SUCH_ELEMENT
import android.telephony.IccOpenLogicalChannelResponse.STATUS_UNKNOWN_ERROR
import im.angry.openeuicc.util.TelephonyManagerShim
import im.angry.openeuicc.util.shim
import net.typeblog.genericapi.Channel

class Session(private val reader: Reader) : net.typeblog.genericapi.Session {
    internal val tm: TelephonyManagerShim by lazy { reader.tm.shim }

    private val slotIndex: Int
        get() = reader.slotIndex

    private val portIndex: Int
        get() = reader.portIndex

    internal val channels = mutableListOf<Channel>()

    override fun getReader() = reader

    override fun isClosed() = !reader.sessions.contains(this)

    override fun closeChannels() {
        Utils.closeAll(channels)
        reader.sessions.remove(this)
    }

    override fun openBasicChannel(aid: ByteArray?, p2: Byte): Channel =
        Channel(session = this, response = null).also { channels.add(it) }

    @OptIn(ExperimentalStdlibApi::class)
    override fun openLogicalChannel(aid: ByteArray, p2: Byte): Channel? {
        val response = iccOpenLogicalChannel(aid.toHexString(), p2.toInt())
        if (response.channel == INVALID_CHANNEL) return null
        return when (response.status) {
            STATUS_MISSING_RESOURCE -> throw Exception("No resources available to open logical channel")
            STATUS_NO_SUCH_ELEMENT -> throw Exception("AID not found on the secure element")
            STATUS_UNKNOWN_ERROR -> throw Exception("Unknown error opening logical channel")
            else -> Channel(session = this, response).also { channels.add(it) }
        }
    }

    // @formatter:off
    internal fun iccTransmitApdu(channel: Int?, cla: Int, ins: Int, p1: Int, p2: Int, p3: Int, data: String?) =
        if (channel == null)
            iccTransmitApduBasicChannel(cla, ins, p1, p2, p3, data) else
            iccTransmitApduLogicalChannel(channel, cla, ins, p1, p2, p3, data)

    private fun iccTransmitApduBasicChannel(cla: Int, ins: Int, p1: Int, p2: Int, p3: Int, data: String?) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            tm.iccTransmitApduBasicChannelByPort(slotIndex, portIndex, cla, ins, p1, p2, p3, data) else
            tm.iccTransmitApduBasicChannelBySlot(slotIndex, cla, ins, p1, p2, p3, data)

    private fun iccTransmitApduLogicalChannel(channel: Int, cla: Int, ins: Int, p1: Int, p2: Int, p3: Int, data: String?) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            tm.iccTransmitApduLogicalChannelByPort(slotIndex, portIndex, channel, cla, ins, p1, p2, p3, data) else
            tm.iccTransmitApduLogicalChannelBySlot(slotIndex, channel,  cla, ins, p1, p2, p3, data)

    private fun iccOpenLogicalChannel(aid: String, p2: Int) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            tm.iccOpenLogicalChannelByPort(slotIndex, portIndex, aid, p2) else
            tm.iccOpenLogicalChannelBySlot(slotIndex, aid, p2)

    internal fun iccCloseLogicalChannel(channel: Int) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            tm.iccCloseLogicalChannelByPort(slotIndex, portIndex, channel) else
            tm.iccCloseLogicalChannelBySlot(slotIndex, channel)
    // @formatter:on
}
