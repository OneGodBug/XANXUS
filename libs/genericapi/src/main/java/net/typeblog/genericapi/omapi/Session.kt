package net.typeblog.genericapi.omapi

import android.se.omapi.Session
import net.typeblog.genericapi.Channel
import net.typeblog.genericapi.Reader

class Session(private val reader: Reader, private val impl: Session) : net.typeblog.genericapi.Session {
    override fun getReader(): Reader = reader

    override fun getATR(): ByteArray? = impl.atr

    override fun close() = impl.close()

    override fun isClosed(): Boolean = impl.isClosed

    override fun closeChannels() = impl.closeChannels()

    override fun openBasicChannel(aid: ByteArray?, p2: Byte): Channel? {
        val channel = impl.openBasicChannel(aid, p2) ?: return null
        return Channel(session = this, impl = channel)
    }

    override fun openLogicalChannel(aid: ByteArray?, p2: Byte): Channel? {
        val channel = impl.openLogicalChannel(aid, p2) ?: return null
        return Channel(session = this, impl = channel)
    }
}
