package de.marmaro.krt.ffupdater.app.entity

import androidx.annotation.Keep

@Keep
enum class InstallationStatus {
    INSTALLED, INSTALLED_WITH_DIFFERENT_FINGERPRINT, NOT_INSTALLED
}