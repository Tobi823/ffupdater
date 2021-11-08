package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import kotlinx.coroutines.runBlocking

/**
 * Allow the user to select an app from the dialog to install.
 * Show warning or error message (if ABI is not supported) if necessary.
 */
class InstallNewAppDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val apps = runBlocking {
            App.values().filterNot {
                it.detail.isInstalled(context)
            }
        }
        val names = apps.map { context.getString(it.detail.displayTitle) }.toTypedArray()
        return AlertDialog.Builder(activity)
                .setTitle(R.string.install_activity__retrigger_installation__button_title)
                .setItems(names) { _: DialogInterface, which: Int ->
                    triggerAppInstallation(apps[which])
                }
                .create()
    }

    private fun triggerAppInstallation(app: App) {
        // do not install an app which incompatible ABIs
        if (DeviceEnvironment.abis.intersect(app.detail.supportedAbis).isEmpty()) {
            UnsupportedAbiDialog.newInstance().show(parentFragmentManager)
            return
        }
        // do not install an app which require a newer Android version
        if (DeviceEnvironment.sdkInt < app.detail.minApiLevel) {
            DeviceTooOldDialog.newInstance(app).show(parentFragmentManager)
            return
        }
        ShowAppInfoBeforeInstallationDialog.newInstance(app).show(parentFragmentManager)
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
    }

    fun show(manager: FragmentManager) {
        show(manager, "install_new_app_dialog")
    }

    companion object {
        fun newInstance(): InstallNewAppDialog {
            return InstallNewAppDialog()
        }
    }
}