package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AppDetail

/**
 * Show a dialog with the app description.
 */
class AppInfoDialog(private val appDetail: AppDetail) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(getString(appDetail.displayTitle))
                .setMessage(getString(appDetail.displayDescription))
                .setPositiveButton(getString(R.string.ok))
                { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "app_info_dialog")
    }
}