package de.marmaro.krt.ffupdater.device

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.Keep

/**
 * This class makes SDK checks testable because Mockk can't mock/change Android classes.
 */
@Keep
object DeviceSdkTester {
    fun supportsAndroid6M23(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.M
    }

    fun supportsAndroid7Nougat24(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.N
    }

    fun supportsAndroid8Oreo26(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.O
    }

    fun supportsAndroid9P28(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.P
    }

    fun supportsAndroid10Q29(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.Q
    }

    fun supportsAndroid11Q30(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.R
    }

    fun supportsAndroid12S31(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.S
    }

    fun supportsAndroid13T33(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun supportsAndroid14U34(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
}