package net.typeblog.genericapi.telephony

import android.telephony.IccOpenLogicalChannelResponse

class Channel(private val session: Session, private val response: IccOpenLogicalChannelResponse?) :
    net.typeblog.genericapi.Channel {

    override fun close() {
        if (response != null) session.iccCloseLogicalChannel(response.channel)
        session.channels.remove(this)
    }

    override fun isBasicChannel() = response == null

    override fun isOpen() = if (isBasicChannel) true else session.channels.contains(this)

    override fun getSelectResponse(): ByteArray? = response?.selectResponse

    override fun getSession() = session

    @OptIn(ExperimentalStdlibApi::class)
    override fun transmit(command: ByteArray): ByteArray {
        if (command.size < 5)
            throw IllegalArgumentException("APDU command must be at least 5 bytes long")
        val (cla, ins, p1, p2, p3) = command.take(5).map(Byte::toInt)
        if (isBasicChannel && (cla and 0x03) != 0x00)
            throw IllegalArgumentException("Cannot use logical channel CLA on basic channel")
        val data = if (command.size > 5)
            command.copyOfRange(5, command.size).toHexString() else
            null
        return session
            .iccTransmitApdu(response?.channel, cla, ins, p1, p2, p3, data)
            .hexToByteArray()
    }

    override fun selectNext() = false
}
