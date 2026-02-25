package im.angry.openeuicc.ui

import android.content.Context
import android.net.http.SslCertificate
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import im.angry.openeuicc.common.R


class SslCertificateFragment : DialogFragment() {
    companion object {
        private const val FIELD_CERTIFICATE = "certificate"

        fun newInstance(certificate: SslCertificate) = SslCertificateFragment().apply {
            arguments = Bundle()
            arguments!!.putBundle(FIELD_CERTIFICATE, SslCertificate.saveState(certificate))
        }
    }

    private val certificate by lazy {
        requireArguments().getBundle(FIELD_CERTIFICATE)!!
            .let(SslCertificate::restoreState)
    }

    private val certificateView by lazy {
        val context = requireContext()
        val names = arrayOf(certificate.issuedTo.dName)
        val nameAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, names).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = true
            addView(Spinner(context).apply { adapter = nameAdapter })
            addView(certificate.inflateCertificateView(context))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setView(certificateView)
            .setTitle(R.string.ssl_certificate)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()

    private fun SslCertificate.inflateCertificateView(context: Context) = this.javaClass
        .getMethod("inflateCertificateView", Context::class.java)
        .invoke(this, context) as View
}
