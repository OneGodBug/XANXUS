package im.angry.openeuicc.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri

class TelephonyProviderFixer(private val context: Context) {
    companion object {
        private val CONTENT_URI = "content://telephony/siminfo".toUri()
    }

    private val contentResolver = context.contentResolver

    fun isPermissionGranted(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
        )
        return permissions
            .map { ActivityCompat.checkSelfPermission(context, it) }
            .all(PackageManager.PERMISSION_GRANTED::equals)
    }

    fun perform(): Int {
        // DELETE FROM siminfo
        // DELETE FROM siminfo WHERE uicc_applications_enabled = 0
        // UPDATE siminfo SET uicc_applications_enabled = 1 WHERE icc_id = ?
        return contentResolver.delete(CONTENT_URI, null, null)
    }
}
