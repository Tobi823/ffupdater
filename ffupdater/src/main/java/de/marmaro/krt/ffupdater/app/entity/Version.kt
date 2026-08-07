package de.marmaro.krt.ffupdater.app.entity

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

/**
 * buildDateTime is sometimes necessary to distinguish two different versions with the same version text (see Firefox Nightly).
 * Normally this value could be set.
 */
@Parcelize
@Keep
data class Version(val versionText: String, val buildDateTime: LocalDateTime?) : Parcelable {
    constructor(versionText: String) : this(versionText, null)
}