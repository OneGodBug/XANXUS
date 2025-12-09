package net.typeblog.lpac_jni

data class ActivationCode(
    val address: String,
    val matchingId: String?,
    val imei: String?,
    val confirmationCode: String?
)
