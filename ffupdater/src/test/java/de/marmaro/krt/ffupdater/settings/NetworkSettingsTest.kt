package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.BaseTest
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class NetworkSettingsTest : BaseTest() {
    private lateinit var sharedPreferences: SharedPreferences

    @BeforeEach
    fun setUp() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        NetworkSettings.init(sharedPreferences)
    }

    @Test
    fun areUserCAsTrusted() {
        SettingsTestHelper.testBooleanSetting(
            sharedPreferences,
            "network__trust_user_cas",
            false
        ) { NetworkSettings.areUserCAsTrusted }
    }
}