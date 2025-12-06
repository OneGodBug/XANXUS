package net.typeblog.genericapi.telephony

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import net.typeblog.genericapi.Session


class Reader(internal val context: Context, internal val slotIndex: Int, internal val portIndex: Int) :
    net.typeblog.genericapi.Reader {
    internal val tm by lazy { context.getSystemService(TelephonyManager::class.java) }

    internal val sm by lazy { context.getSystemService(SubscriptionManager::class.java) }

    internal val sessions = mutableListOf<Session>()

    val subscriptionInfo: SubscriptionInfo?
        get() = sm.activeSubscriptionInfoList?.firstOrNull {
            it.simSlotIndex == slotIndex && it.portIndex == portIndex
        }

    override fun getName() = "TM$slotIndex:$portIndex"

    override fun isSecureElementPresent() = subscriptionInfo
        ?.let { tm.createForSubscriptionId(it.subscriptionId).hasIccCard() }
        ?: false

    override fun openSession() =
        Session(reader = this).also { sessions.add(it) }

    override fun closeSessions() = Utils.closeAll(sessions)
}
