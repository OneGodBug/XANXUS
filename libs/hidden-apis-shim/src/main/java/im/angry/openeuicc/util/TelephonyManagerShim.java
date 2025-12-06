package im.angry.openeuicc.util;

import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.UiccSlotMapping;

import java.util.Collection;

interface TelephonyManagerShim {
    IccOpenLogicalChannelResponse iccOpenLogicalChannelBySlot(int slotIndex, String aid, int p2);

    IccOpenLogicalChannelResponse iccOpenLogicalChannelByPort(int slotIndex, int portIndex, String aid, int p2);

    boolean iccCloseLogicalChannelBySlot(int slotIndex, int channel);

    void iccCloseLogicalChannelByPort(int slotIndex, int portIndex, int channel);

    String iccTransmitApduBasicChannelBySlot(int slotIndex, int cla, int ins, int p1, int p2, int p3, String data);

    String iccTransmitApduBasicChannelByPort(int slotIndex, int portIndex, int cla, int ins, int p1, int p2, int p3, String data);

    String iccTransmitApduLogicalChannelBySlot(int slotIndex, int channel, int cla, int ins, int p1, int p2, int p3, String data);

    String iccTransmitApduLogicalChannelByPort(int slotIndex, int portIndex, int channel, int cla, int ins, int p1, int p2, int p3, String data);

    Collection<UiccSlotMapping> getSimSlotMapping();

    void setSimSlotMapping(Collection<UiccSlotMapping> slotMapping);
}
