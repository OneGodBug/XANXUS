package im.angry.openeuicc.util

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccSlotMapping
import java.lang.reflect.Method
import java.lang.reflect.Proxy

// Hidden APIs via reflection to enable building without AOSP source tree
// When building against AOSP, this file can be simply excluded to resolve
// calls to AOSP hidden APIs
private val tmMethods by lazy {
    val parent = TelephonyManager::class.java
    TelephonyManagerShim::class.java.declaredMethods
        .map { parent.getMethod(it.name, *it.parameterTypes) }
        .associateBy { it.name }
}

private val smMethods by lazy {
    val parent = SubscriptionManager::class.java
    SubscriptionManagerShim::class.java.declaredMethods
        .map { parent.getMethod(it.name, *it.parameterTypes) }
        .associateBy { it.name }
}

// @formatter:off
fun TelephonyManager.iccOpenLogicalChannelBySlot(slotId: Int, appletId: String?, p2: Int) =
    tmMethods["iccOpenLogicalChannelBySlot"]!!.invoke(this, slotId, appletId, p2) as IccOpenLogicalChannelResponse

fun TelephonyManager.iccOpenLogicalChannelByPort(slotId: Int, portId: Int, appletId: String?, p2: Int) =
    tmMethods["iccOpenLogicalChannelByPort"]!!.invoke(this, slotId, portId, appletId, p2) as IccOpenLogicalChannelResponse

fun TelephonyManager.iccCloseLogicalChannelBySlot(slotId: Int, channel: Int) {
    tmMethods["iccCloseLogicalChannelBySlot"]!!.invoke(this, slotId, channel)
}

fun TelephonyManager.iccCloseLogicalChannelByPort(slotId: Int, portId: Int, channel: Int) {
    tmMethods["iccCloseLogicalChannelByPort"]!!.invoke(this, slotId, portId, channel)
}

fun TelephonyManager.iccTransmitApduLogicalChannelBySlot(slotId: Int, channel: Int, cla: Int, ins: Int, p1: Int, p2: Int, p3: Int, data: String?) =
    tmMethods["iccTransmitApduLogicalChannelBySlot"]!!.invoke(this, slotId, channel, cla, ins, p1, p2, p3, data) as String?

fun TelephonyManager.iccTransmitApduLogicalChannelByPort(slotId: Int, portId: Int, channel: Int, cla: Int, ins: Int, p1: Int, p2: Int, p3: Int, data: String?) =
    tmMethods["iccTransmitApduLogicalChannelByPort"]!!.invoke(this, slotId, portId, channel, cla, ins, p1, p2, p3, data) as String?

@Suppress("UNCHECKED_CAST")
var TelephonyManager.simSlotMapping: Collection<UiccSlotMapping>
    get() = tmMethods["getSimSlotMapping"]!!.invoke(this) as Collection<UiccSlotMapping>
    set(value) {
        tmMethods["setSimSlotMapping"]!!.invoke(this, value)
    }

fun SubscriptionManager.requestEmbeddedSubscriptionInfoListRefresh(cardId: Int) {
    smMethods["requestEmbeddedSubscriptionInfoListRefresh"]!!.invoke(this, cardId)
}
