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
 * Ask the user with this dialog if he really want to install the app.
 */
class ShowWarningBeforeInstallation : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val app = App.valueOf(requireArguments().getString(BUNDLE_APP_NAME)!!)
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.installation_warning_title))
                .setMessage(getString(app.detail.displayWarning!!))
                .setPositiveButton(getString(R.string.installation_warning_positive_button)) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    (activity as MainActivity).downloadApp(app)
                }
                .setNegativeButton(getString(R.string.installation_warning_negative_button)) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "show_warning_before_installation_dialog")
    }

    companion object {
        private const val BUNDLE_APP_NAME = "app_name"

        fun newInstance(app: App): ShowWarningBeforeInstallation {
            val bundle = Bundle()
            bundle.putString(BUNDLE_APP_NAME, app.name)
            val fragment = ShowWarningBeforeInstallation()
            fragment.arguments = bundle
            return fragment
        }
    }
}