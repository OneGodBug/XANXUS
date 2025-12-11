package im.angry.openeuicc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.preference.Preference
import im.angry.easyeuicc.R
import im.angry.openeuicc.util.encodeHex
import java.security.MessageDigest

class UnprivilegedSettingsFragment : SettingsFragment() {
    private val signers by lazy {
        val packageInfo = with(requireContext()) {
            packageManager.getPackageInfo(packageName, /* flags = */ PackageManager.GET_SIGNING_CERTIFICATES)
        }
        packageInfo.signingInfo!!.apkContentsSigners
            .map { MessageDigest.getInstance("SHA-1").digest(it.toByteArray()) }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.pref_unprivileged_settings)
        mergePreferenceOverlay("pref_info_overlay", "pref_info")

        requirePreference<Preference>("pref_info_ara_m").apply {
            summary = signers.first().encodeHex()
            setOnPreferenceClickListener {
                requireContext().getSystemService(ClipboardManager::class.java)!!
                    .setPrimaryClip(ClipData.newPlainText("ara-m", summary))
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Toast
                    .makeText(requireContext(), R.string.toast_ara_m_copied, Toast.LENGTH_SHORT)
                    .show()
                true
            }
        }

        requirePreference<Preference>("pref_info_website").apply {
            if (!isVisible || intent == null) return@apply
            val allSigners = Base64.encodeToString(
                /* input = */ signers.reduce(ByteArray::plus),
                /* flags = */ Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            intent!!.data = intent!!.data!!.buildUpon()
                .appendQueryParameter("k", allSigners)
                .build()
        }
    }
}
