package net.typeblog.genericapi.omapi

class Reader(private val impl: android.se.omapi.Reader) : net.typeblog.genericapi.Reader {
    override fun getName() =
        impl.name

    override fun isSecureElementPresent() =
        impl.isSecureElementPresent

    override fun openSession() =
        Session(reader = this, impl.openSession())

    override fun closeSessions() =
        impl.closeSessions()
}
