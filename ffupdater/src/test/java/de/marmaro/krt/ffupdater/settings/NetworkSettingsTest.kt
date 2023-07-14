package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class NetworkSettingsTest {
    private lateinit var sharedPreferences: SharedPreferences

    @BeforeEach
    fun setUp() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        NetworkSettings.init(sharedPreferences)
    }

    @Test
    fun areUserCAsTrusted_withDefault_returnFalse() {
        assertFalse(NetworkSettings.areUserCAsTrusted)
    }

    @Test
    fun areUserCAsTrusted_withTrue_returnTrue() {
        val pref = SPMockBuilder().createSharedPreferences()
        pref.edit()
            .putBoolean("network__trust_user_cas", true)
            .apply()
        NetworkSettings.init(pref)
        assertTrue(NetworkSettings.areUserCAsTrusted)
    }

    @Test
    fun areUserCAsTrusted_withFalse_returnFalse() {
        sharedPreferences.edit().putBoolean("network__trust_user_cas", false).commit()
        assertFalse(NetworkSettings.areUserCAsTrusted)
    }

    @Test
    fun areUserCAsTrusted_withChangingValue_returnCorrectValue() {
        sharedPreferences.edit().putBoolean("network__trust_user_cas", true).commit()
        assertTrue(NetworkSettings.areUserCAsTrusted)

        sharedPreferences.edit().putBoolean("network__trust_user_cas", false).commit()
        assertFalse(NetworkSettings.areUserCAsTrusted)

        sharedPreferences.edit().putBoolean("network__trust_user_cas", true).commit()
        assertTrue(NetworkSettings.areUserCAsTrusted)

        sharedPreferences.edit().putBoolean("network__trust_user_cas", false).commit()
        assertFalse(NetworkSettings.areUserCAsTrusted)
    }
}