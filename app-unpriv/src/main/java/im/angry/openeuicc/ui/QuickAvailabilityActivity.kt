package im.angry.openeuicc.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import im.angry.easyeuicc.R
import im.angry.openeuicc.di.UnprivilegedUiComponentFactory
import im.angry.openeuicc.util.OpenEuiccContextMarker

class QuickAvailabilityActivity : AppCompatActivity(), OpenEuiccContextMarker {
    companion object {
        const val EXTRA_FROM = "from_quick_availability"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quick_availability)

        val quickAvailabilityFragment =
            (appContainer.uiComponentFactory as UnprivilegedUiComponentFactory)
                .createQuickAvailabilityFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.quick_availability_container, quickAvailabilityFragment)
            .commit()
    }

    fun launchMainActivity() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)!!
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(EXTRA_FROM, true)
        startActivity(intent)
        finish()
    }
}
