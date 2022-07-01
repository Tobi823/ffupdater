package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.installer.entity.Installer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class InstallerSettingsHelperTest {

    @MockK
    lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @BeforeEach
    fun setUp() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
    }

    companion object {
        @JvmStatic
        fun testDataForInstaller(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, Installer.SESSION_INSTALLER),
            Arguments.of(null, null, false, Installer.SESSION_INSTALLER),
            Arguments.of(null, null, true, Installer.ROOT_INSTALLER),

            Arguments.of(null, false, null, Installer.SESSION_INSTALLER),
            Arguments.of(null, false, false, Installer.SESSION_INSTALLER),
            Arguments.of(null, false, true, Installer.ROOT_INSTALLER),

            Arguments.of(null, true, null, Installer.NATIVE_INSTALLER),
            Arguments.of(null, true, false, Installer.NATIVE_INSTALLER),
            Arguments.of(null, true, true, Installer.ROOT_INSTALLER),

            Arguments.of(false, null, null, Installer.SESSION_INSTALLER),
            Arguments.of(false, null, false, Installer.SESSION_INSTALLER),
            Arguments.of(false, null, true, Installer.ROOT_INSTALLER),

            Arguments.of(false, false, null, Installer.SESSION_INSTALLER),
            Arguments.of(false, false, false, Installer.SESSION_INSTALLER),
            Arguments.of(false, false, true, Installer.ROOT_INSTALLER),

            Arguments.of(false, true, null, Installer.NATIVE_INSTALLER),
            Arguments.of(false, true, false, Installer.NATIVE_INSTALLER),
            Arguments.of(false, true, true, Installer.ROOT_INSTALLER),

            Arguments.of(true, null, null, Installer.SESSION_INSTALLER),
            Arguments.of(true, null, false, Installer.SESSION_INSTALLER),
            Arguments.of(true, null, true, Installer.ROOT_INSTALLER),

            Arguments.of(true, false, null, Installer.SESSION_INSTALLER),
            Arguments.of(true, false, false, Installer.SESSION_INSTALLER),
            Arguments.of(true, false, true, Installer.ROOT_INSTALLER),

            Arguments.of(true, true, null, Installer.NATIVE_INSTALLER),
            Arguments.of(true, true, false, Installer.NATIVE_INSTALLER),
            Arguments.of(true, true, true, Installer.ROOT_INSTALLER),
        )
    }

    @ParameterizedTest(
        name = "returns getInstaller() \"{3}\" if installer__session is \"{0}\", " +
                "installer__native is \"{1}\" and installer__root is \"{2}\""
    )
    @MethodSource("testDataForInstaller")
    fun `getInstaller()`(
        installer__session: Boolean?,
        installer__native: Boolean?,
        installer__root: Boolean?,
        expected: Installer
    ) {
        if (installer__session != null) {
            sharedPreferences.edit().putBoolean("installer__session", installer__session).commit()
        }
        if (installer__native != null) {
            sharedPreferences.edit().putBoolean("installer__native", installer__native).commit()
        }
        if (installer__root != null) {
            sharedPreferences.edit().putBoolean("installer__root", installer__root).commit()
        }
        val sut = InstallerSettingsHelper(context)
        assertEquals(expected, sut.getInstaller())
    }
}