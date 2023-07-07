package de.marmaro.krt.ffupdater.security

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class Sha256Hash(val hexValue: String) : Parcelable