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

/**
 * Allow the user to select an app from the dialog to install.
 * Show warning or error message (if ABI is not supported) if necessary.
 */
class InstallAppDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val apps = App.values().filterNot { it.detail.isInstalled(context) }
        val names = apps.map { context.getString(it.detail.displayTitle) }.toTypedArray()
        return AlertDialog.Builder(activity)
                .setTitle(R.string.install_application)
                .setItems(names) { _: DialogInterface, which: Int ->
                    triggerAppInstallation(apps[which])
                }
                .create()
    }

    private fun triggerAppInstallation(app: App) {
        val deviceEnvironment = DeviceEnvironment()
        // do not install an app which incompatible ABIs
        if (deviceEnvironment.abis.intersect(app.detail.supportedAbis).isEmpty()) {
            UnsupportedAbiDialog.newInstance().show(parentFragmentManager)
            return
        }
        // do not install an app which require a newer Android version
        if (deviceEnvironment.sdkInt < app.detail.minApiLevel) {
            DeviceTooOldDialog.newInstance(app).show(parentFragmentManager)
            return
        }
        ShowAppInfoBeforeInstallationDialog.newInstance(app).show(parentFragmentManager)
    }

    fun show(manager: FragmentManager) {
        show(manager, "install_app_dialog")
    }

    companion object {
        fun newInstance(): InstallAppDialog {
            return InstallAppDialog()
        }
    }
}