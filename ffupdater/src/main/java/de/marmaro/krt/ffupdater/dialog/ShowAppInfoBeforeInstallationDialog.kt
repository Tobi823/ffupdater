package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App

/**
 * Show a dialog with the app description.
 */
class ShowAppInfoBeforeInstallationDialog(
        private val app: App,
        private val installCallback: (App) -> Unit,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(getString(app.detail.displayTitle))
                .setMessage(getString(app.detail.displayDescription))
                .setPositiveButton(getString(R.string.install_app))
                { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    if (app.detail.displayWarning != null) {
                        ShowWarningBeforeInstallation(app, installCallback)
                                .show(parentFragmentManager)
                    } else {
                        installCallback.invoke(app)
                    }
                }
                .setNegativeButton(getString(R.string.go_back)) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "show_app_info_before_installation_dialog")
    }
}