package de.marmaro.krt.ffupdater.installer

data class InstallResult(
    val success: Boolean,
    val certificateHash: String? = null,
    val errorCode: Int? = null,
    val errorMessage: String? = null,
    val errorException: Throwable? = null
)