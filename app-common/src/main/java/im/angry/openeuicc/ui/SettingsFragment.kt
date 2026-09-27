package im.angry.openeuicc.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

open class SettingsFragment : PreferenceFragmentCompat(), OpenEuiccContextMarker {
    private lateinit var developerPref: PreferenceCategory

    // Hidden developer options switch
    private var numClicks = 0
    private var lastClickTimestamp = -1L
    private var lastToast: Toast? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)

        developerPref = requirePreference("pref_developer")

        // Show / hide developer preference based on whether it is enabled
        lifecycleScope.launch {
            preferenceRepository.developerOptionsEnabledFlow
                .onEach(developerPref::setVisible)
                .collect()
        }

        requirePreference<Preference>("pref_info_app_version").apply {
            summary = requireContext().selfAppVersion

            // Enable developer options when this is clicked for 7 times
            setOnPreferenceClickListener(::onAppVersionClicked)
        }

        requirePreference<Preference>("pref_advanced_language").apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+：保持原来的系统语言设置
                val uri = Uri.fromParts(
                    "package",
                    requireContext().packageName,
                    null
                )

                intent = Intent(
                    Settings.ACTION_APP_LOCALE_SETTINGS,
                    uri
                )

                isVisible =
                    intent!!.resolveActivity(
                        requireContext().packageManager
                    ) != null
            } else {
                // Android 12 及以下：使用自己的语言选择器
                isVisible = true

                setOnPreferenceClickListener {
                    showLanguageDialog()
                    true
                }
            }
        }

        requirePreference<Preference>("pref_advanced_logs").apply {
            intent = Intent(requireContext(), LogsActivity::class.java)
        }

        requirePreference<CheckBoxPreference>("pref_notifications_download")
            .bindBooleanFlow(preferenceRepository.notificationDownloadFlow)

        requirePreference<CheckBoxPreference>("pref_notifications_delete")
            .bindBooleanFlow(preferenceRepository.notificationDeleteFlow)

        requirePreference<CheckBoxPreference>("pref_notifications_switch")
            .bindBooleanFlow(preferenceRepository.notificationSwitchFlow)

        requirePreference<CheckBoxPreference>("pref_advanced_disable_safeguard_removable_esim")
            .bindBooleanFlow(preferenceRepository.disableSafeguardFlow)

        requirePreference<CheckBoxPreference>("pref_advanced_verbose_logging")
            .bindBooleanFlow(preferenceRepository.verboseLoggingFlow)

        requirePreference<CheckBoxPreference>("pref_advanced_force_tpdu_mode")
            .bindBooleanFlow(preferenceRepository.forceTpduModeFlow)

        requirePreference<EditTextPreference>("pref_advanced_http_proxy")
            .bindStringFlow(preferenceRepository.httpProxyFlow)

        requirePreference<CheckBoxPreference>("pref_developer_unfiltered_profile_list")
            .bindBooleanFlow(preferenceRepository.unfilteredProfileListFlow)

        requirePreference<CheckBoxPreference>("pref_developer_ignore_tls_certificate")
            .bindBooleanFlow(preferenceRepository.ignoreTLSCertificateFlow)

        requirePreference<CheckBoxPreference>("pref_developer_refresh_after_switch")
            .bindBooleanFlow(preferenceRepository.refreshAfterSwitchFlow)

        requirePreference<ListPreference>("pref_developer_es10x_mss")
            .bindIntFlow(preferenceRepository.es10xMssFlow, 63)

        requirePreference<Preference>("pref_developer_isdr_aid_list").apply {
            intent = Intent(requireContext(), IsdrAidListActivity::class.java)
        }

        requirePreference<Preference>("pref_info_website").apply {
            val uri = appContainer.customizableTextProvider.websiteUri
                ?: return@apply

            isVisible = true
            summary = uri.buildUpon().clearQuery().build().toString()
            intent = Intent(
                Intent.ACTION_VIEW,
                uri
            )
        }
    }

    protected fun <T : Preference> requirePreference(key: CharSequence) =
        findPreference<T>(key)!!

    private fun showLanguageDialog() {
        val languages = arrayOf(
            "跟随系统",
            "简体中文",
            "繁體中文",
            "日本語"
        )

        val locales = arrayOf(
            "",
            "zh-CN",
            "zh-TW",
            "ja"
        )

        val currentLocale =
            AppCompatDelegate
                .getApplicationLocales()
                .toLanguageTags()

        val currentIndex = when (currentLocale) {
            "" -> 0
            "zh-CN" -> 1
            "zh-TW" -> 2
            "ja" -> 3
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.pref_advanced_language)
            .setSingleChoiceItems(
                languages,
                currentIndex
            ) { dialog, which ->

                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(
                        locales[which]
                    )
                )

                dialog.dismiss()
            }
            .show()
    }

    override fun onStart() {
        super.onStart()

        setupRootViewSystemBarInsets(
            requireView(),
            arrayOf(
                mainViewPaddingInsetHandler(
                    requireView().requireViewById(R.id.recycler_view)
                )
            )
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAppVersionClicked(pref: Preference): Boolean {
        if (developerPref.isVisible) return false

        val now = System.currentTimeMillis()

        numClicks =
            if (now - lastClickTimestamp >= 1000) {
                1
            } else {
                numClicks + 1
            }

        lastClickTimestamp = now

        lifecycleScope.launch {
            preferenceRepository.developerOptionsEnabledFlow
                .updatePreference(numClicks >= 7)
        }

        val toastText = when {
            numClicks == 7 ->
                getString(R.string.developer_options_enabled)

            numClicks > 1 ->
                getString(
                    R.string.developer_options_steps,
                    7 - numClicks
                )

            else -> return true
        }

        lastToast?.cancel()

        lastToast = Toast.makeText(
            requireContext(),
            toastText,
            Toast.LENGTH_SHORT
        )

        lastToast!!.show()

        return true
    }

    protected fun CheckBoxPreference.bindBooleanFlow(
        flow: PreferenceFlowWrapper<Boolean>
    ) {
        lifecycleScope.launch {
            flow.collect(::setChecked)
        }

        setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                flow.updatePreference(newValue as Boolean)
            }

            true
        }
    }

    private fun EditTextPreference.bindStringFlow(
        flow: PreferenceFlowWrapper<String>
    ) {
        lifecycleScope.launch {
            flow.collect(::setText)
        }

        setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                flow.updatePreference(newValue as String)
            }

            true
        }
    }

    private fun ListPreference.bindIntFlow(
        flow: PreferenceFlowWrapper<Int>,
        defaultValue: Int
    ) {
        lifecycleScope.launch {
            flow.collect {
                value = it.toString()
            }
        }

        setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                flow.updatePreference(
                    (newValue as String).toIntOrNull()
                        ?: defaultValue
                )
            }

            true
        }
    }

    protected fun mergePreferenceOverlay(
        overlayKey: String,
        targetKey: String
    ) {
        val overlayCat =
            requirePreference<PreferenceCategory>(overlayKey)

        val targetCat =
            requirePreference<PreferenceCategory>(targetKey)

        val prefs = buildList {
            for (i in 0..<overlayCat.preferenceCount) {
                add(overlayCat.getPreference(i))
            }
        }

        prefs.forEach {
            overlayCat.removePreference(it)
            targetCat.addPreference(it)
        }

        overlayCat.parent?.removePreference(overlayCat)
    }
}
