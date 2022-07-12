package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App

/**
 * Ask the user with this dialog if he really want to install the app.
 */
class AppWarningBeforeInstallationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appName = requireNotNull(requireArguments().getString(BUNDLE_APP_NAME))
        val app = App.valueOf(appName)
        val warning = getString(app.detail.installationWarning!!)
        val counter = warning.lines()
            .filter { it.startsWith("- ") }
            .count()
        val message = resources.getQuantityString(
            R.plurals.app_warning_before_installation_dialog__installation_question,
            counter,
            getString(app.detail.title),
            warning
        )

        return AlertDialog.Builder(activity)
            .setTitle(R.string.app_warning_before_installation_dialog__title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_button__yes) { dialog, _ ->
                dialog.dismiss()
                AppInfoBeforeInstallationDialog.newInstance(app).show(parentFragmentManager)
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
        show(manager, "app_warning_before_installation_dialog")
    }

    companion object {
        private const val BUNDLE_APP_NAME = "app_name"

        fun newInstance(app: App): AppWarningBeforeInstallationDialog {
            requireNotNull(app.detail.installationWarning)
            val bundle = Bundle()
            bundle.putString(BUNDLE_APP_NAME, app.name)
            val fragment = AppWarningBeforeInstallationDialog()
            fragment.arguments = bundle
            return fragment
        }
    }
}