package im.angry.openeuicc.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

internal object UnprivilegedPreferenceKeys {
    // ---- Miscellaneous ----
    val SKIP_QUICK_COMPATIBILITY = booleanPreferencesKey("skip_quick_compatibility")

    val FREE_DROID_WARNING_ACKNOWLEDGED = intPreferencesKey("free_droid_warning_acknowledged")
}

class UnprivilegedPreferenceRepository(context: Context) : PreferenceRepository(context) {
    // ---- Miscellaneous ----
    val skipQuickCompatibilityFlow = bindFlow(UnprivilegedPreferenceKeys.SKIP_QUICK_COMPATIBILITY, false)

    val freeDroidWarningAcknowledgedFlow = bindFlow(UnprivilegedPreferenceKeys.FREE_DROID_WARNING_ACKNOWLEDGED, 0)
}
