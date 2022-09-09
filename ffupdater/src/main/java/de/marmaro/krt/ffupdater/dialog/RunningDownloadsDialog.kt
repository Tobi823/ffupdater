package de.marmaro.krt.ffupdater.dialog

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.DownloadActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App

class RunningDownloadsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appName = requireNotNull(requireArguments().getString(BUNDLE_APP_NAME))
        val app = App.valueOf(appName)
        return AlertDialog.Builder(activity)
            .setTitle(R.string.running_downloads_dialog__title)
            .setMessage(R.string.running_downloads_dialog__message)
            .setPositiveButton(R.string.running_downloads_dialog__yes) { dialog, _ ->
                dialog.dismiss()
                val intent = DownloadActivity.createIntent(requireContext(), app)
                startActivity(intent)
                if (requireArguments().getBoolean(BUNDLE_FINISH_ACTIVITY_WHEN_INSTALLING)) {
                    (requireContext() as Activity).finish()
                }
            }
            .setNegativeButton(R.string.running_downloads_dialog__negative) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod =
            LinkMovementMethod.getInstance()
    }

    fun show(manager: FragmentManager) {
        show(manager, "downloads_are_running_dialog")
    }

    companion object {
        private const val BUNDLE_APP_NAME = "app_name"
        private const val BUNDLE_FINISH_ACTIVITY_WHEN_INSTALLING = "finish_activity_when_installing"

        fun newInstance(app: App, finishActivityWhenInstalling: Boolean): RunningDownloadsDialog {
            val bundle = Bundle()
            bundle.putString(BUNDLE_APP_NAME, app.name)
            bundle.putBoolean(BUNDLE_FINISH_ACTIVITY_WHEN_INSTALLING, finishActivityWhenInstalling)
            val fragment = RunningDownloadsDialog()
            fragment.arguments = bundle
            return fragment
        }
    }
}