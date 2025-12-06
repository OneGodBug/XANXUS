package net.typeblog.genericapi.telephony

import java.io.Closeable

internal object Utils {
    fun closeAll(iterable: MutableIterable<Closeable>) {
        val iterator = iterable.iterator()
        while (iterator.hasNext()) {
            iterator.next().close()
            iterator.remove()
        }
    }
}
