package de.marmaro.krt.ffupdater.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import de.marmaro.krt.ffupdater.DownloadActivity
import de.marmaro.krt.ffupdater.FFUpdater
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.layout.cardview_option_dialog
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.AppBase
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

    var hideAutomaticUpdateSwitch: Boolean = false
    var appHasDifferentSignature: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(cardview_option_dialog, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appImpl = app.findImpl()
        configureTitle(view, appImpl)
        configureUrl(view, appImpl)
        configureEoL(view, appImpl)
        configureDiffSignature(view)
        configureDescription(view, appImpl)
        configureWarnings(view, appImpl)
        configureUpdateSwitch(view, appImpl)
        configureExitButton(view)
        configureInstallButton(view)
    }

    private fun configureTitle(view: View, appImpl: AppBase) {
        view.findViewById<TextView>(R.id.cardview_dialog__title).text = getString(appImpl.title)
    }

    private fun configureUrl(view: View, appImpl: AppBase) {
        view.findViewById<TextView>(R.id.cardview_dialog__url).text = appImpl.projectPage
    }

    private fun configureEoL(view: View, appImpl: AppBase) {
        val label = view.findViewById<TextView>(R.id.cardview_dialog__eol_label)
        val text = view.findViewById<TextView>(R.id.cardview_dialog__eol)
        label.visibility = if (appImpl.isEol()) View.VISIBLE else View.GONE
        text.visibility = if (appImpl.isEol()) View.VISIBLE else View.GONE
        text.text = appImpl.eolReason?.let { getString(it) }
    }

    private fun configureDiffSignature(view: View) {
        val label = view.findViewById<TextView>(R.id.cardview_dialog__different_signature_label)
        label.visibility = if (appHasDifferentSignature) View.VISIBLE else View.GONE
    }

    private fun configureDescription(view: View, appImpl: AppBase) {
        val textViewDescription = view.findViewById<TextView>(R.id.cardview_dialog__description)
        textViewDescription.text = getString(appImpl.description)
    }

    private fun configureWarnings(view: View, appImpl: AppBase) {
        val label = view.findViewById<TextView>(R.id.cardview_dialog__warnings_label)
        val text = view.findViewById<TextView>(R.id.cardview_dialog__warnings)
        val warnings = appImpl.installationWarning?.let { getString(it) }
        label.visibility = if (warnings == null) View.GONE else View.VISIBLE
        text.visibility = if (warnings == null) View.GONE else View.VISIBLE
        text.text = warnings ?: ""
    }

    private fun configureUpdateSwitch(view: View, appImpl: AppBase) {
        val switchUpdate = view.findViewById<MaterialSwitch>(R.id.cardview_dialog__auto_bg_updates_switch)
        switchUpdate.visibility = if (hideAutomaticUpdateSwitch) View.GONE else View.VISIBLE
        switchUpdate.isChecked = app !in BackgroundSettings.excludedAppsFromUpdateCheck
        switchUpdate.isEnabled = !appImpl.isEol() && !appHasDifferentSignature
        switchUpdate.setOnCheckedChangeListener { _, isChecked ->
            val excludeApp = !isChecked
            setFragmentResult(AUTO_UPDATE_CHANGED, Bundle())
            BackgroundSettings.setAppToBeExcludedFromUpdateCheck(app, excludeApp)
        }
    }

    private fun configureExitButton(view: View) {
        val buttonExit = view.findViewById<MaterialButton>(R.id.cardview_dialog__exit_button)
        buttonExit.setOnClickListener { dismiss() }
    }

    private fun configureInstallButton(view: View) {
        val buttonInstall = view.findViewById<MaterialButton>(R.id.cardview_dialog__install_button)
        buttonInstall.isEnabled = !appHasDifferentSignature
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
        const val AUTO_UPDATE_CHANGED = "AUTO_UPDATE_CHANGED"
    }
}