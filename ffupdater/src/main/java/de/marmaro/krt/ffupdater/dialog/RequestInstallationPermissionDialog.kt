package de.marmaro.krt.ffupdater.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.marmaro.krt.ffupdater.BuildConfig
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.string.request_installation_permission_dialog__abort

@Keep
@RequiresApi(api = Build.VERSION_CODES.O)
class RequestInstallationPermissionDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
            .setTitle(R.string.request_installation_permission_dialog__title)
            .setMessage(R.string.request_installation_permission_dialog__message)
            .setPositiveButton(R.string.request_installation_permission_dialog__grant_permission) { dialog, _ ->
                dialog.dismiss()
                val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                val requestPermission = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
                startActivity(requestPermission)
            }
            .setNegativeButton(request_installation_permission_dialog__abort) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod =
            LinkMovementMethod.getInstance()
    }

    fun show(manager: FragmentManager) {
        show(manager, "request_installation_permission_dialog")
    }

    companion object {
        fun newInstance(): InstallSameVersionDialog {
            return InstallSameVersionDialog()
        }
    }
}