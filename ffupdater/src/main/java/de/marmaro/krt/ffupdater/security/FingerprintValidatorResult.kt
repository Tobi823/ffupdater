package de.marmaro.krt.ffupdater.security

import androidx.annotation.Keep

@Keep
data class FingerprintValidatorResult(
    val isValid: Boolean,
    val hexString: String,
)