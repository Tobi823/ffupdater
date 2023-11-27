package de.marmaro.krt.ffupdater.dialog

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import de.marmaro.krt.ffupdater.DownloadActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.layout.app_installation_info_dialog
import de.marmaro.krt.ffupdater.R.layout.cardview_option_dialog
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.ForegroundSettings


/**
 * Show a dialog with the app description.
 */
@Keep
class AppInstallationInfoDialog(private val app: App) : AppCompatDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(app_installation_info_dialog, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appImpl = app.findImpl()
        val textViewTitle = view.findViewById<TextView>(R.id.cardview_dialog__title)
        val textViewUrl = view.findViewById<TextView>(R.id.cardview_dialog__url)
        val textViewWarningsLabel = view.findViewById<TextView>(R.id.cardview_dialog__warnings_label)
        val textViewWarnings = view.findViewById<TextView>(R.id.cardview_dialog__warnings)
        val buttonExit = view.findViewById<MaterialButton>(R.id.cardview_dialog__exit_button)
        val buttonInstall = view.findViewById<MaterialButton>(R.id.cardview_dialog__install_button)

        textViewTitle.text = getString(appImpl.title)
        textViewUrl.text = appImpl.projectPage
        view.findViewById<TextView>(R.id.cardview_dialog__description).text = getString(appImpl.description)

        val warnings = appImpl.installationWarning?.let { getString(it) }
        textViewWarningsLabel.visibility = if (warnings == null) View.GONE else View.VISIBLE
        textViewWarnings.visibility = if (warnings == null) View.GONE else View.VISIBLE
        textViewWarnings.text = warnings ?: ""

        buttonExit.setOnClickListener { dismiss() }
        buttonInstall.setOnClickListener { installApp(app) }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        setStyle(STYLE_NO_FRAME, R.style.Theme_Material3_DayNight_Dialog_Alert)
        super.show(manager, tag)
    }

    private fun installApp(app: App) {
        val context = requireContext()
        if (!ForegroundSettings.isUpdateCheckOnMeteredAllowed && NetworkUtil.isNetworkMetered(context)) {
            //showToast(R.string.main_activity__no_unmetered_network)
            return
        }
        if (DeviceSdkTester.supportsAndroid8Oreo26() && !context.packageManager.canRequestPackageInstalls()) {
            RequestInstallationPermissionDialog().show(childFragmentManager)
            return
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            // this may updates the app
            RunningDownloadsDialog(app).show(childFragmentManager)
            return
        }
        val intent = DownloadActivity.createIntent(context, app)
        context.startActivity(intent)
        dismiss()
    }

    fun show(manager: FragmentManager) {
        show(manager, "cardview_options_dialog")
    }
}