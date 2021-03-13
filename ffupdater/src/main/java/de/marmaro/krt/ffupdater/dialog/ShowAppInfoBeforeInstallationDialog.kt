package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.MainActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App

/**
 * Show a dialog with the app description.
 */
class ShowAppInfoBeforeInstallationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val app = App.valueOf(requireArguments().getString(BUNDLE_APP_NAME)!!)
        return AlertDialog.Builder(activity)
                .setTitle(getString(app.detail.displayTitle))
                .setMessage(getString(app.detail.displayDescription))
                .setPositiveButton(getString(R.string.install_app))
                { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    if (app.detail.displayWarning != null) {
                        ShowWarningBeforeInstallation.newInstance(app).show(parentFragmentManager)
                    } else {
                        (activity as MainActivity).downloadApp(app)
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

    companion object {
        private const val BUNDLE_APP_NAME = "app_name"

        fun newInstance(app: App): ShowAppInfoBeforeInstallationDialog {
            val bundle = Bundle()
            bundle.putString(BUNDLE_APP_NAME, app.name)
            val fragment = ShowAppInfoBeforeInstallationDialog()
            fragment.arguments = bundle
            return fragment
        }
    }
}