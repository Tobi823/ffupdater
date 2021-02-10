package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.utils.Utils

/**
 * Show the user that the app could not be installed because the operating system is too old.
 */
class DeviceTooOldDialog(private val app: App,
                         private val deviceEnvironment: DeviceEnvironment) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(R.string.device_too_old_dialog_title)
                .setMessage(getString(R.string.device_too_old_dialog_message,
                        Utils.getVersionAndCodenameForApiLevel(app.detail.minApiLevel),
                        Utils.getVersionAndCodenameForApiLevel(deviceEnvironment.sdkInt)))
                .setNegativeButton(getString(R.string.ok)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
    }

    fun show(manager: FragmentManager) {
        show(manager, "device_too_old_dialog")
    }
}