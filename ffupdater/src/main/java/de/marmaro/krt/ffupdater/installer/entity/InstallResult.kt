package de.marmaro.krt.ffupdater.installer.entity

import androidx.annotation.Keep

@Keep
data class InstallResult(
    val certificateHash: String? = null,
)