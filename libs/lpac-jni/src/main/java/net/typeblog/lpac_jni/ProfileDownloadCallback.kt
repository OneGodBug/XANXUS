package net.typeblog.lpac_jni

data class ProfileDownloadMetadata (
    val iccid: String,
    val profileName: String,
    val providerName: String,
    val profileClass: ProfileClass
) {
    constructor(iccid: String, profileName: String, providerName: String, profileClass: String):
        this(iccid, profileName, providerName, ProfileClass.fromString(profileClass))
}

data class ProfileInstallationResult (
    val iccid: String?,
    val notificationSeqNumber: Long,
);

fun interface ProfileDownloadCallback {
    fun onStateUpdate(state: ProfileDownloadState, profileDownloadMetadata: ProfileDownloadMetadata?, profileInstallationResult: ProfileInstallationResult?)
}
