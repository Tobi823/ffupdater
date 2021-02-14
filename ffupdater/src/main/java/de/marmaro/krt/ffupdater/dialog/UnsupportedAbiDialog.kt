package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R

/**
 * Show the error that the app is not supported on this smartphone.
 */
class UnsupportedAbiDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(R.string.unsupported_abi_dialog_title)
                .setMessage(R.string.unsupported_abi_dialog_message)
                .setNegativeButton(R.string.ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "unsupported_abi_dialog")
    }
}