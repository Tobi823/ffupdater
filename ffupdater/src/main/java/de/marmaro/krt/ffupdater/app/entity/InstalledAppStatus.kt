package de.marmaro.krt.ffupdater.app.entity

import android.os.Parcelable
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class InstalledAppStatus(
    val app: App,
    val latestVersion: LatestVersion,
    val isUpdateAvailable: Boolean,
    val displayVersion: String,
    val objectCreationTimestamp: Long = System.currentTimeMillis(),
) : Parcelable