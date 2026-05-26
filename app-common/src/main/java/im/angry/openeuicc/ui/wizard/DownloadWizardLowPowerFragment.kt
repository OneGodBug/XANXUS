package im.angry.openeuicc.ui.wizard

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.angry.openeuicc.common.R

class DownloadWizardLowPowerFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    companion object {
        fun isBatteryLow(context: Context): Boolean {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter) ?: return false
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            return level <= 20 // battery level is considered low if it's 20% or less
        }
    }

    override val hasNext: Boolean
        get() = true

    override val hasPrev: Boolean
        get() = true

    override fun createNextFragment() = DownloadWizardSlotSelectFragment()

    override fun createPrevFragment() = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_download_low_power, container, /* attachToRoot = */ false)
}
