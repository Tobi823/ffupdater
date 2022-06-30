package de.marmaro.krt.ffupdater.security

data class FingerprintValidatorResult(
    val isValid: Boolean,
    val hexString: String,
)