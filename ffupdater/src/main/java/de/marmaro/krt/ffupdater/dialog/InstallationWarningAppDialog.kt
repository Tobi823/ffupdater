package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.util.Consumer
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App

/**
 * Ask the user with this dialog if he really want to install the app.
 */
class InstallationWarningAppDialog internal constructor(private val downloadCallback: Consumer<App>,
                                                        private val app: App) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.installation_warning_title))
                .setMessage(app.detail.displayWarning?.let { getString(it) } ?: "")
                .setPositiveButton(getString(R.string.installation_warning_positive_button))
                { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    downloadCallback.accept(app)
                }
                .setNegativeButton(getString(R.string.installation_warning_negative_button))
                { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "warning_app_dialog")
    }
}