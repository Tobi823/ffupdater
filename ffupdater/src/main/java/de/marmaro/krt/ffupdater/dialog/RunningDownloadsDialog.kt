package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.Keep
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import de.marmaro.krt.ffupdater.activity.download.DownloadActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App

@Keep
class RunningDownloadsDialog(private val app: App) : DialogFragment() {

    @Throws(IllegalArgumentException::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
            .setTitle(R.string.running_downloads_dialog__title)
            .setMessage(R.string.running_downloads_dialog__message)
            .setPositiveButton(R.string.running_downloads_dialog__yes) { dialog, _ ->
                setFragmentResult(DOWNLOAD_ACTIVITY_WAS_STARTED, Bundle())
                dialog.dismiss()
                val intent = DownloadActivity.createIntent(requireContext(), app)
                startActivity(intent)
            }
            .setNegativeButton(R.string.running_downloads_dialog__negative) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "downloads_are_running_dialog")
    }

    companion object {
        const val DOWNLOAD_ACTIVITY_WAS_STARTED = "DOWNLOAD_ACTIVITY_WAS_STARTED"
    }
}