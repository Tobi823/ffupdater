package de.marmaro.krt.ffupdater.security

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Sha256Hash(
    val hexValue: String,
) : Parcelable