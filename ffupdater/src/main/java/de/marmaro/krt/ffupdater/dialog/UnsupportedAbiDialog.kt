package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.Keep
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R

/**
 * Show the error that the app is not supported on this smartphone.
 */
@Keep
class UnsupportedAbiDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
            .setTitle(R.string.unsupported_abi_dialog__title)
            .setMessage(R.string.unsupported_abi_dialog__message)
            .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "unsupported_abi_dialog")
    }
}