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
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.R.layout.cardview_option_dialog
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.settings.BackgroundSettings


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

        textViewTitle.text = getString(appImpl.title)
        textViewUrl.text = appImpl.projectPage
        view.findViewById<TextView>(R.id.cardview_dialog__description).text = getString(appImpl.description)

        val warnings = appImpl.installationWarning?.let { getString(it) }
        textViewWarningsLabel.visibility = if (warnings == null) View.GONE else View.VISIBLE
        textViewWarnings.visibility = if (warnings == null) View.GONE else View.VISIBLE
        textViewWarnings.text = warnings ?: ""

        switchUpdate.isChecked = app !in BackgroundSettings.excludedAppsFromUpdateCheck
        switchUpdate.setOnCheckedChangeListener { _, isChecked ->
            BackgroundSettings.setAppToBeExcludedFromUpdateCheck(app, isChecked)
        }

        buttonExit.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod =
            LinkMovementMethod.getInstance()
    }

    fun show(manager: FragmentManager) {
        show(manager, "cardview_options_dialog")
    }

    companion object {
        fun newInstance(app: App): CardviewOptionsDialog {
            val fragment = CardviewOptionsDialog(app)
            fragment.setStyle(STYLE_NO_FRAME, R.style.Theme_Material3_DayNight_Dialog_Alert)
            return fragment
        }
    }
}