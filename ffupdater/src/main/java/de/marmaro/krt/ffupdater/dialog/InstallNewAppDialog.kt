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
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.utils.ifTrue

/**
 * Allow the user to select an app from the dialog to install.
 * Show warning or error message (if ABI is not supported) if necessary.
 */
class InstallNewAppDialog(
    private val deviceAbiExtractor: DeviceAbiExtractor,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val apps = App.values()
            .filter { app -> app.impl.showAsInstallable() }
            .filterNot { app -> app.impl.isInstalled(context) }
        val names = apps.map { app -> context.getString(app.impl.title) }
            .toTypedArray()

        return AlertDialog.Builder(activity)
            .setTitle(R.string.install_new_app)
            .setItems(names) { _, which -> triggerAppInstallation(apps[which]) }
            .create()
    }

    private fun triggerAppInstallation(app: App) {
        // do not install an app which incompatible ABIs
        deviceAbiExtractor.supportedAbis
            .none { abi -> abi in app.impl.supportedAbis }
            .ifTrue { UnsupportedAbiDialog.newInstance().show(parentFragmentManager); return }

        // do not install an app which require a newer Android version
        if (DeviceSdkTester.sdkInt < app.impl.minApiLevel) {
            DeviceTooOldDialog.newInstance(app).show(parentFragmentManager)
            return
        }

        if (app.impl.installationWarning != null) {
            AppWarningBeforeInstallationDialog.newInstance(app).show(parentFragmentManager)
            return
        }

        AppInfoBeforeInstallationDialog.newInstance(app).show(parentFragmentManager)
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod =
            LinkMovementMethod.getInstance()
    }

    fun show(manager: FragmentManager) {
        show(manager, "install_new_app_dialog")
    }

    companion object {
        fun newInstance(deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE): InstallNewAppDialog {
            return InstallNewAppDialog(deviceAbiExtractor)
        }
    }
}