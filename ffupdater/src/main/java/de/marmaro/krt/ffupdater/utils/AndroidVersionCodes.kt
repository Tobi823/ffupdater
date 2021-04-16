package de.marmaro.krt.ffupdater.utils

import android.os.Build.VERSION_CODES.*

/**
 * Class with useful helper methods.
 */
object AndroidVersionCodes {

    /**
     * @param apiLevel API Level
     * @return the Android version an its codename for the associated API Level
     */
    @JvmStatic
    fun getVersionForApiLevel(apiLevel: Int): String {
        return when (apiLevel) {
            LOLLIPOP -> "5.0 (Lollipop)"
            LOLLIPOP_MR1 -> "5.1 (Lollipop)"
            M -> "6.0 (Marshmallow)"
            N -> "7.0 (Nougat)"
            N_MR1 -> "7.1 (Nougat)"
            O -> "8.0.0 (Oreo)"
            O_MR1 -> "8.1.0 (Oreo)"
            P -> "9 (Pie)"
            Q -> "10 (Q)"
            R -> "11 (R)"
            R + 1 -> throw Exception("missing entry for Android 12")
            else -> throw Exception("invalid API level")
        }
    }
}