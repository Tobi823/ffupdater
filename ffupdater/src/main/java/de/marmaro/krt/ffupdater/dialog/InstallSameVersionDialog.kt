package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.annotation.Keep
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.DownloadActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.network.file.FileDownloader

@Keep
class InstallSameVersionDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appName = requireNotNull(requireArguments().getString(BUNDLE_APP_NAME))
        val app = App.valueOf(appName)
        return AlertDialog.Builder(activity)
            .setTitle(R.string.install_same_version_dialog__title)
            .setMessage(R.string.install_same_version_dialog__message)
            .setPositiveButton(R.string.dialog_button__yes) { dialog, _ ->
                dialog.dismiss()
                if (FileDownloader.areDownloadsCurrentlyRunning()) {
                    RunningDownloadsDialog.newInstance(app, false).show(parentFragmentManager)
                } else {
                    val intent = DownloadActivity.createIntent(requireContext(), app)
                    startActivity(intent)
                }
            }
            .setNegativeButton(R.string.dialog_button__do_not_install) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod =
            LinkMovementMethod.getInstance()
    }

    fun show(manager: FragmentManager) {
        show(manager, "install_same_version_dialog")
    }

    companion object {
        private const val BUNDLE_APP_NAME = "app_name"

        fun newInstance(app: App): InstallSameVersionDialog {
            val bundle = Bundle()
            bundle.putString(BUNDLE_APP_NAME, app.name)
            val fragment = InstallSameVersionDialog()
            fragment.arguments = bundle
            return fragment
        }
    }
}