package de.marmaro.krt.ffupdater.installer.entity

data class ShortInstallResult(
    val success: Boolean,
    val errorCode: Int? = null,
    val errorMessage: String? = null
)