package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.MaintainedApp

/**
 * Show a dialog with the app warning.
 */
class AppWarningDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appName = requireNotNull(requireArguments().getString(BUNDLE_APP_NAME))
        val app = MaintainedApp.valueOf(appName)
        val warning = getString(app.detail.displayWarning!!)
        val counter = warning.lines()
            .filter { it.startsWith("- ") }
            .count()
        val message = resources.getQuantityString(
            R.plurals.app_warning_dialog__message,
            counter,
            getString(app.detail.title),
            warning
        )
        return AlertDialog.Builder(activity)
            .setTitle(getString(R.string.app_warning_dialog__title))
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod =
            LinkMovementMethod.getInstance()
    }

    fun show(manager: FragmentManager) {
        show(manager, "app_warning_dialog")
    }

    companion object {
        private const val BUNDLE_APP_NAME = "app_name"

        fun newInstance(app: MaintainedApp): AppWarningDialog {
            requireNotNull(app.detail.displayWarning)
            val bundle = Bundle()
            bundle.putString(BUNDLE_APP_NAME, app.name)
            val fragment = AppWarningDialog()
            fragment.arguments = bundle
            return fragment
        }
    }
}