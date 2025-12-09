package im.angry.openeuicc.ui.wizard

import android.os.Parcel
import android.os.Parcelable
import net.typeblog.lpac_jni.LocalProfileAssistant

data class DownloadWizardState(
    var currentStepFragmentClassName: String? = null,
    var selectedSyntheticSlotId: Int,
    var smdp: String = "",
    var matchingId: String? = null,
    var confirmationCode: String? = null,
    var imei: String? = null,
    var downloadStarted: Boolean = false,
    var downloadTaskID: Long = -1L,
    var downloadError: LocalProfileAssistant.ProfileDownloadException? = null,
    var skipMethodSelect: Boolean = false,
    var confirmationCodeRequired: Boolean = false,
) : Parcelable {
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<DownloadWizardState> {
            override fun createFromParcel(source: Parcel) = DownloadWizardState(
                currentStepFragmentClassName = source.readString(),
                selectedSyntheticSlotId = source.readInt(),
                smdp = checkNotNull(source.readString()) { "hostname is null" },
                matchingId = source.readString(),
                confirmationCode = source.readString(),
                imei = source.readString(),
                downloadStarted = source.readByte() != 0.toByte(),
                downloadTaskID = source.readLong(),
                downloadError = null,
                skipMethodSelect = source.readByte() != 0.toByte(),
                confirmationCodeRequired = source.readByte() != 0.toByte()
            )

            override fun newArray(size: Int) = arrayOfNulls<DownloadWizardState?>(size)
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(currentStepFragmentClassName)
        dest.writeInt(selectedSyntheticSlotId)
        dest.writeString(smdp)
        dest.writeString(matchingId)
        dest.writeString(confirmationCode)
        dest.writeString(imei)
        dest.writeByte(if (downloadStarted) 1 else 0)
        dest.writeLong(downloadTaskID)
        // Note: downloadError is not parceled
        dest.writeByte(if (skipMethodSelect) 1 else 0)
        dest.writeByte(if (confirmationCodeRequired) 1 else 0)
    }
}
