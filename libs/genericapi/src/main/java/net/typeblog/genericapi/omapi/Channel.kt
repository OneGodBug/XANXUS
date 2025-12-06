package net.typeblog.genericapi.omapi

import android.se.omapi.Channel
import net.typeblog.genericapi.Session

class Channel(private val session: Session, private val impl: Channel) : net.typeblog.genericapi.Channel {
    override fun close() =
        impl.close()

    override fun isBasicChannel() =
        impl.isBasicChannel

    override fun isOpen() =
        impl.isOpen

    override fun getSelectResponse() =
        impl.selectResponse

    override fun getSession() =
        session

    override fun transmit(command: ByteArray) =
        impl.transmit(command)

    override fun selectNext(): Boolean =
        impl.selectNext()
}
