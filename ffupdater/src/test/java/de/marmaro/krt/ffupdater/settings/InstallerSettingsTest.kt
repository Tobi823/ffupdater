package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.BaseTest
import de.marmaro.krt.ffupdater.installer.entity.Installer
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class InstallerSettingsTest : BaseTest() {

    private lateinit var sharedPreferences: SharedPreferences

    @BeforeEach
    fun setUp() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        InstallerSettings.init(sharedPreferences)
    }

    companion object {
        @JvmStatic
        fun testDataForInstaller(): Stream<Arguments> = Stream.of(
            Arguments.of(null, Installer.SESSION_INSTALLER),
            Arguments.of("", Installer.SESSION_INSTALLER),
            Arguments.of("invalid", Installer.SESSION_INSTALLER),
            Arguments.of("NATIVE_INSTALLER", Installer.NATIVE_INSTALLER),
            Arguments.of("ROOT_INSTALLER", Installer.ROOT_INSTALLER),
            Arguments.of("SHIZUKU_INSTALLER", Installer.SHIZUKU_INSTALLER),
        )
    }

    @ParameterizedTest(
        name = "returns getInstaller() \"{1}\" if string is \"{0}\""
    )
    @MethodSource("testDataForInstaller")
    fun `getInstaller()`(
        string_value: String?,
        expected: Installer,
    ) {
        sharedPreferences.edit().putString("installer__method", string_value).apply()
        assertEquals(expected, InstallerSettings.getInstallerMethod())
    }
}