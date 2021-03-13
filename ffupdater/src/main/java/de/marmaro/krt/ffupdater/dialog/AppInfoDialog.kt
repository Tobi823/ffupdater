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
class AppInfoDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val app = App.valueOf(requireArguments().getString(BUNDLE_APP_NAME)!!)
        return AlertDialog.Builder(activity)
                .setTitle(getString(app.detail.displayTitle))
                .setMessage(getString(app.detail.displayDescription))
                .setPositiveButton(getString(R.string.ok))
                { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "app_info_dialog")
    }

    companion object {
        private const val BUNDLE_APP_NAME = "app_name"

        fun newInstance(app: App): AppInfoDialog {
            val bundle = Bundle()
            bundle.putString(BUNDLE_APP_NAME, app.name)
            val fragment = AppInfoDialog()
            fragment.arguments = bundle
            return fragment
        }
    }
}