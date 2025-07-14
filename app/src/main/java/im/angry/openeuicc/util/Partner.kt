package im.angry.openeuicc.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import im.angry.openeuicc.BuildConfig


class Partner {
    companion object {
        private const val ACTION = "com.google.android.euicc.action.PARTNER_CUSTOMIZATION"

        private val instances = mutableMapOf<String, Partner?>()

        private val packageFlags: Int
            get() {
                val flags = if (BuildConfig.DEBUG)
                    PackageManager.MATCH_UNINSTALLED_PACKAGES else
                    PackageManager.MATCH_SYSTEM_ONLY
                return flags or PackageManager.MATCH_DISABLED_COMPONENTS
            }

        fun getInstance(context: Context, action: String = ACTION) = instances.getOrPut(action) {
            context.packageManager
                .queryBroadcastReceivers(Intent(action), packageFlags)
                .mapNotNull { it.activityInfo?.applicationInfo }
                .firstNotNullOfOrNull {
                    try {
                        context.packageManager.getResourcesForApplication(it)
                    } catch (_: PackageManager.NameNotFoundException) {
                        null
                    }
                }
                ?.let(::Partner)
        }
    }

    private val resources: Resources

    private constructor(resources: Resources) {
        this.resources = resources
    }

    private fun getIdentifier(name: String) =
        resources.getIdentifier(name, null, null).takeIf { it != 0 }

    fun getString(name: String) = getIdentifier(name)?.let(resources::getString)

    fun getBoolean(name: String) = getIdentifier(name)?.let(resources::getBoolean)
}