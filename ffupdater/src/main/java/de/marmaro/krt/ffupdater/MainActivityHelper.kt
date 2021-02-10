package de.marmaro.krt.ffupdater

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.App.*

class MainActivityHelper(private val activity: Activity) {
    fun getAppCardViewForApp(app: App): CardView {
        return activity.findViewById(when (app) {
            FIREFOX_KLAR -> R.id.firefoxKlarCard
            FIREFOX_FOCUS -> R.id.firefoxFocusCard
            FIREFOX_LITE -> R.id.firefoxLiteCard
            FIREFOX_RELEASE -> R.id.firefoxReleaseCard
            FIREFOX_BETA -> R.id.firefoxBetaCard
            FIREFOX_NIGHTLY -> R.id.firefoxNightlyCard
            LOCKWISE -> R.id.lockwiseCard
            BRAVE -> R.id.braveCard
            ICERAVEN -> R.id.iceravenCard
        })
    }

    fun enableDownloadButton(app: App) {
        getDownloadButtonForApp(app).setImageResource(R.drawable.ic_file_download_orange)
    }

    fun disableDownloadButton(app: App) {
        getDownloadButtonForApp(app).setImageResource(R.drawable.ic_file_download_grey)
    }

    fun getDownloadButtonForApp(app: App): ImageButton {
        return activity.findViewById(when (app) {
            FIREFOX_KLAR -> R.id.firefoxKlarDownloadButton
            FIREFOX_FOCUS -> R.id.firefoxFocusDownloadButton
            FIREFOX_LITE -> R.id.firefoxLiteDownloadButton
            FIREFOX_RELEASE -> R.id.firefoxReleaseDownloadButton
            FIREFOX_BETA -> R.id.firefoxBetaDownloadButton
            FIREFOX_NIGHTLY -> R.id.firefoxNightlyDownloadButton
            LOCKWISE -> R.id.lockwiseDownloadButton
            BRAVE -> R.id.braveDownloadButton
            ICERAVEN -> R.id.iceravenDownloadButton
        })
    }

    fun getInstalledVersionTextView(app: App): TextView {
        return activity.findViewById(when (app) {
            FIREFOX_KLAR -> R.id.firefoxKlarInstalledVersion
            FIREFOX_FOCUS -> R.id.firefoxFocusInstalledVersion
            FIREFOX_LITE -> R.id.firefoxLiteInstalledVersion
            FIREFOX_RELEASE -> R.id.firefoxReleaseInstalledVersion
            FIREFOX_BETA -> R.id.firefoxBetaInstalledVersion
            FIREFOX_NIGHTLY -> R.id.firefoxNightlyInstalledVersion
            LOCKWISE -> R.id.lockwiseInstalledVersion
            BRAVE -> R.id.braveInstalledVersion
            ICERAVEN -> R.id.iceravenInstalledVersion
        })
    }

    fun getAvailableVersionTextView(app: App): TextView {
        return activity.findViewById(when (app) {
            FIREFOX_KLAR -> R.id.firefoxKlarAvailableVersion
            FIREFOX_FOCUS -> R.id.firefoxFocusAvailableVersion
            FIREFOX_LITE -> R.id.firefoxLiteAvailableVersion
            FIREFOX_RELEASE -> R.id.firefoxReleaseAvailableVersion
            FIREFOX_BETA -> R.id.firefoxBetaAvailableVersion
            FIREFOX_NIGHTLY -> R.id.firefoxNightlyAvailableVersion
            LOCKWISE -> R.id.lockwiseAvailableVersion
            BRAVE -> R.id.braveAvailableVersion
            ICERAVEN -> R.id.iceravenAvailableVersion
        })
    }

    fun getInfoButtonForApp(app: App): View {
        return activity.findViewById(when (app) {
            FIREFOX_KLAR -> R.id.firefoxKlarInfoButton
            FIREFOX_FOCUS -> R.id.firefoxFocusInfoButton
            FIREFOX_LITE -> R.id.firefoxLiteInfoButton
            FIREFOX_RELEASE -> R.id.firefoxReleaseInfoButton
            FIREFOX_BETA -> R.id.firefoxBetaInfoButton
            FIREFOX_NIGHTLY -> R.id.firefoxNightlyInfoButton
            LOCKWISE -> R.id.lockwiseInfoButton
            BRAVE -> R.id.braveInfoButton
            ICERAVEN -> R.id.iceravenInfoButton
        })
    }
}