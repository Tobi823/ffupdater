package de.marmaro.krt.ffupdater.installer.entity

import androidx.annotation.Keep

// must match installer__method__values
@Keep
enum class Installer {
    SESSION_INSTALLER,
    NATIVE_INSTALLER,
    ROOT_INSTALLER,
    SHIZUKU_INSTALLER,
}