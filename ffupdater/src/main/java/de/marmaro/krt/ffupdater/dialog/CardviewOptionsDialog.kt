package de.marmaro.krt.ffupdater.dialog

import android.content.Context
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
import de.marmaro.krt.ffupdater.activity.download.DownloadActivity
import de.marmaro.krt.ffupdater.FFUpdater
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.layout.cardview_dialog
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus.INSTALLED_WITH_DIFFERENT_FINGERPRINT
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import kotlinx.coroutines.runBlocking


/**
 * Show a dialog with the app description.
 */
@Keep
class CardviewOptionsDialog : AppCompatDialogFragment() {
    private lateinit var app: App
    private lateinit var appImpl: AppBase
    private var isEol = false
    private var isExcluded = false
    private var wrongFingerprint = false
    private var installedByOtherApp = true
    private var hideAutomaticUpdateSwitch = false
    private var hideTheHideButton = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initAttributes()
        return inflater.inflate(cardview_dialog, container)
    }

    private fun initAttributes() {
        val appName = requireArguments().getString(argsApp)!!
        app = App.valueOf(appName)
        appImpl = app.findImpl()
        isEol = appImpl.isEol()
        isExcluded = app in BackgroundSettings.excludedAppsFromUpdateCheck
        runBlocking {
            wrongFingerprint = appImpl.isInstalled(requireContext()) == INSTALLED_WITH_DIFFERENT_FINGERPRINT
        }
        installedByOtherApp = appImpl.wasInstalledByOtherApp(requireContext())
        hideAutomaticUpdateSwitch = requireArguments().getBoolean(argsHideAutomaticUpdateSwitch)
        hideTheHideButton = requireArguments().getBoolean(argsHideTheHideButton)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureTitle(view)
        configureUrl(view)
        configureEoL(view)
        configureNotInstalledByFFUpdaterLabel(view)
        configureDiffSignature(view)
        configureDescription(view)
        configureWarnings(view)
        configureUpdateSwitch(view)
        configureExitButton(view)
        configureHideButton(view)
        configureInstallButton(view)
    }

    private fun configureTitle(view: View) {
        view.findViewById<TextView>(R.id.cardview_dialog__title).text = getString(appImpl.title)
    }

    private fun configureUrl(view: View) {
        view.findViewById<TextView>(R.id.cardview_dialog__url).text = appImpl.projectPage
    }

    private fun configureEoL(view: View) {
        val label = view.findViewById<TextView>(R.id.cardview_dialog__eol_label)
        val text = view.findViewById<TextView>(R.id.cardview_dialog__eol)
        label.visibility = if (isEol) View.VISIBLE else View.GONE
        text.visibility = if (isEol) View.VISIBLE else View.GONE
        text.text = appImpl.eolReason?.let { getString(it) }
    }

    private fun configureNotInstalledByFFUpdaterLabel(view: View) {
        val label = view.findViewById<TextView>(R.id.cardview_dialog__not_installed_by_ffupdater_label)
        label.visibility = if (installedByOtherApp && !isEol && !wrongFingerprint) View.VISIBLE else View.GONE
    }

    private fun configureDiffSignature(view: View) {
        val label = view.findViewById<TextView>(R.id.cardview_dialog__different_signature_label)
        label.visibility = if (wrongFingerprint) View.VISIBLE else View.GONE
    }

    private fun configureDescription(view: View) {
        val textViewDescription = view.findViewById<TextView>(R.id.cardview_dialog__description)
        textViewDescription.text = getString(appImpl.description)
    }

    private fun configureWarnings(view: View) {
        val label = view.findViewById<TextView>(R.id.cardview_dialog__warnings_label)
        val text = view.findViewById<TextView>(R.id.cardview_dialog__warnings)
        val warnings = appImpl.installationWarning?.let { getString(it) }
        label.visibility = if (warnings == null) View.GONE else View.VISIBLE
        text.visibility = if (warnings == null) View.GONE else View.VISIBLE
        text.text = warnings ?: ""
    }

    private fun configureUpdateSwitch(view: View) {
        val switchUpdate = view.findViewById<MaterialSwitch>(R.id.cardview_dialog__auto_bg_updates_switch)
        switchUpdate.visibility = if (hideAutomaticUpdateSwitch) View.GONE else View.VISIBLE
        switchUpdate.isChecked = !isExcluded && !isEol && !wrongFingerprint && !installedByOtherApp
        switchUpdate.isEnabled = !isEol && !wrongFingerprint && !installedByOtherApp
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

    private fun configureHideButton(view: View) {
        val hideButton = view.findViewById<MaterialButton>(R.id.cardview_dialog__hide_button)
        hideButton.visibility = if (hideTheHideButton) View.GONE else View.VISIBLE
        hideButton.setOnClickListener {
            setFragmentResult(APP_WAS_HIDDEN, Bundle())
            ForegroundSettings.hideApp(app)
            dismiss()
        }
    }

    private fun configureInstallButton(view: View) {
        val buttonInstall = view.findViewById<MaterialButton>(R.id.cardview_dialog__install_button)
        buttonInstall.isEnabled = !wrongFingerprint
        buttonInstall.setOnClickListener { installLatestUpdate() }
    }

    fun show(manager: FragmentManager, tempContext: Context) {
        setStyle(STYLE_NO_FRAME, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog_Alert)
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
            dialog.setFragmentResultListener(RunningDownloadsDialog.DOWNLOAD_ACTIVITY_WAS_STARTED) { _, _ ->
                setFragmentResult(DOWNLOAD_ACTIVITY_WAS_STARTED, Bundle())
                dismiss()
            }
            return
        }
        Log.d(FFUpdater.LOG_TAG, "MainActivity: Start DownloadActivity to install or update ${app.name}.")
        val intent = DownloadActivity.createIntent(context, app)
        startActivity(intent)
        setFragmentResult(DOWNLOAD_ACTIVITY_WAS_STARTED, Bundle())
        dismiss()
    }

    private fun isNetworkUnsuitableForDownload(): Boolean {
        if (ForegroundSettings.isUpdateCheckOnMeteredAllowed) {
            return false
        }
        return NetworkUtil.isNetworkMetered(requireContext())
    }

    fun hideAutomaticUpdateSwitch() {
        requireArguments().putBoolean(argsHideAutomaticUpdateSwitch, true)
    }

    fun hideTheHideButton() {
        requireArguments().putBoolean(argsHideTheHideButton, true)
    }

    companion object {
        fun create(app: App): CardviewOptionsDialog {
            val bundle = Bundle()
            bundle.putString(argsApp, app.name)
            bundle.putBoolean(argsHideAutomaticUpdateSwitch, false)
            bundle.putBoolean(argsHideTheHideButton, false)
            val dialog = CardviewOptionsDialog()
            dialog.arguments = bundle
            return dialog
        }

        private const val argsApp = "app"
        private const val argsHideAutomaticUpdateSwitch = "hideAutomaticUpdateSwitch"
        private const val argsHideTheHideButton = "hideTheHideButton"
        const val AUTO_UPDATE_CHANGED = "AUTO_UPDATE_CHANGED"
        const val DOWNLOAD_ACTIVITY_WAS_STARTED = "DOWNLOAD_ACTIVITY_WAS_STARTED"
        const val APP_WAS_HIDDEN = "APP_WAS_HIDDEN"
    }
}