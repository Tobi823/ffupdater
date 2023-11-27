package de.marmaro.krt.ffupdater.dialog

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.DownloadActivity
import de.marmaro.krt.ffupdater.FFUpdater
import de.marmaro.krt.ffupdater.R
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
class CardviewOptionsDialog(private val app: App) : AppCompatDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(cardview_option_dialog, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appImpl = app.findImpl()
        val textViewTitle = view.findViewById<TextView>(R.id.cardview_dialog__title)
        val textViewUrl = view.findViewById<TextView>(R.id.cardview_dialog__url)
        val textViewWarningsLabel = view.findViewById<TextView>(R.id.cardview_dialog__warnings_label)
        val textViewWarnings = view.findViewById<TextView>(R.id.cardview_dialog__warnings)
        val switchUpdate = view.findViewById<MaterialSwitch>(R.id.cardview_dialog__auto_bg_updates_switch)
        val buttonExit = view.findViewById<MaterialButton>(R.id.cardview_dialog__exit_button)
        val buttonInstall = view.findViewById<MaterialButton>(R.id.cardview_dialog__install_button)

        textViewTitle.text = getString(appImpl.title)
        textViewUrl.text = appImpl.projectPage
        view.findViewById<TextView>(R.id.cardview_dialog__description).text = getString(appImpl.description)

        val warnings = appImpl.installationWarning?.let { getString(it) }
        textViewWarningsLabel.visibility = if (warnings == null) View.GONE else View.VISIBLE
        textViewWarnings.visibility = if (warnings == null) View.GONE else View.VISIBLE
        textViewWarnings.text = warnings ?: ""

        switchUpdate.isChecked = app !in BackgroundSettings.excludedAppsFromUpdateCheck
        switchUpdate.setOnCheckedChangeListener { _, isChecked ->
            val excludeApp = !isChecked
            BackgroundSettings.setAppToBeExcludedFromUpdateCheck(app, excludeApp)
        }

        buttonExit.setOnClickListener { dismiss() }
        buttonInstall.setOnClickListener { installLatestUpdate() }
    }

    fun show(manager: FragmentManager) {
        setStyle(STYLE_NO_FRAME, R.style.Theme_Material3_DayNight_Dialog_Alert)
        show(manager, "cardview_options_dialog")
    }

    private fun installLatestUpdate() {
        val context = requireContext()
        if (isNetworkUnsuitableForDownload()) {
            Toast.makeText(context, R.string.main_activity__no_unmetered_network, Toast.LENGTH_LONG).show()
            return
        }
        if (DeviceSdkTester.supportsAndroid8Oreo26() && !context.packageManager.canRequestPackageInstalls()) {
            RequestInstallationPermissionDialog().show(childFragmentManager)
            return
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            val dialog = RunningDownloadsDialog(app)
            dialog.show(childFragmentManager)
            dialog.setFragmentResultListener(RunningDownloadsDialog.DOWNLOAD_ACTIVITY_WAS_STARTED) { _, _ -> dismiss() }
            return
        }
        Log.d(FFUpdater.LOG_TAG, "MainActivity: Start DownloadActivity to install or update ${app.name}.")
        val intent = DownloadActivity.createIntent(context, app)
        startActivity(intent)
        dismiss()
    }

    private fun isNetworkUnsuitableForDownload(): Boolean {
        if (ForegroundSettings.isUpdateCheckOnMeteredAllowed) {
            return false
        }
        return NetworkUtil.isNetworkMetered(requireContext())
    }

    companion object {
        fun newInstance(app: App): CardviewOptionsDialog {
            val fragment = CardviewOptionsDialog(app)
            fragment.setStyle(STYLE_NO_FRAME, R.style.Theme_Material3_DayNight_Dialog_Alert)
            return fragment
        }
    }
}