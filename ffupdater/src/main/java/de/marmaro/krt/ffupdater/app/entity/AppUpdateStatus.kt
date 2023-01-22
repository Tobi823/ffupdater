package de.marmaro.krt.ffupdater.app.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppUpdateStatus(
    val latestUpdate: LatestUpdate,
    val isUpdateAvailable: Boolean,
    val displayVersion: String,
    val objectCreationTimestamp: Long = System.currentTimeMillis(),
) : Parcelable