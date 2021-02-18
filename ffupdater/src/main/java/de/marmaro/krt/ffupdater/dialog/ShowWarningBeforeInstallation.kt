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
class ShowWarningBeforeInstallation(
        private val app: App,
        private val installCallback: (App) -> Unit,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.installation_warning_title))
                .setMessage(getString(app.detail.displayWarning!!))
                .setPositiveButton(getString(R.string.installation_warning_positive_button)) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    installCallback.invoke(app)
                }
                .setNegativeButton(getString(R.string.installation_warning_negative_button)) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "show_warning_before_installation_dialog")
    }
}