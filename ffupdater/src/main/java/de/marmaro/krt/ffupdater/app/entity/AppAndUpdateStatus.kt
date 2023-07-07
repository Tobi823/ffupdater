package de.marmaro.krt.ffupdater.app.entity

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App

@Keep
data class AppAndUpdateStatus(val app: App, val updateStatus: AppUpdateStatus)
