package im.angry.openeuicc.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import im.angry.easyeuicc.BuildConfig
import im.angry.easyeuicc.R
import im.angry.openeuicc.util.UnprivilegedEuiccContextMarker
import im.angry.openeuicc.util.UnprivilegedPreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FreeDroidFragment : DialogFragment(), UnprivilegedEuiccContextMarker {
    companion object {
        const val TAG = "FreeDroidFragment"

        suspend fun shouldShowWarning(repository: UnprivilegedPreferenceRepository) =
            BuildConfig.VERSION_CODE > repository.freeDroidWarningAcknowledgedFlow.first()

        fun newInstance() = FreeDroidFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext(), im.angry.openeuicc.common.R.style.AlertDialogTheme)
            .setMessage(getString(R.string.free_droid_message, getString(R.string.app_name)))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = BuildConfig.VERSION_CODE
                runBlocking { preferenceRepository.freeDroidWarningAcknowledgedFlow.updatePreference(value) }
            }
            .setNegativeButton(R.string.free_droid_details) { _, _ ->
                val uri = getString(R.string.free_droid_details_url).toUri()
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            .create()
}

