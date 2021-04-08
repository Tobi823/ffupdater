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

class InstallSameVersionDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val app = App.valueOf(requireArguments().getString(BUNDLE_APP_NAME)!!)
        val mainActivity = activity as MainActivity
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.install_same_version_dialog__title))
                .setMessage(getString(R.string.install_same_version_dialog__message))
                .setPositiveButton(getString(R.string.dialog_button__yes)) { dialog: DialogInterface, _: Int ->
                    mainActivity.installApp(app)
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.dialog_button__do_not_install)) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "install_same_version_dialog")
    }

    companion object {
        private const val BUNDLE_APP_NAME = "app_name"

        fun newInstance(app: App): InstallSameVersionDialog {
            val bundle = Bundle()
            bundle.putString(BUNDLE_APP_NAME, app.name)
            val fragment = InstallSameVersionDialog()
            fragment.arguments = bundle
            return fragment
        }
    }
}