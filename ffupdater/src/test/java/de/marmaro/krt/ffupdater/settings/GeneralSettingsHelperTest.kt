package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class GeneralSettingsHelperTest {

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
        fun testDataForBooleanSettings(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "isRootUsageEnabled",
                "general__use_root",
                false,
                { helper: GeneralSettingsHelper -> helper.isRootUsageEnabled })
        )
    }

    @ParameterizedTest(name = "has \"{0}\" the correct default value \"{2}\"")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct default value`(
        name: String,
        preferenceKey: String,
        defaultValue: Boolean,
        getValue: (GeneralSettingsHelper) -> Boolean,
    ) {
        val sut = GeneralSettingsHelper(context)
        val actual = getValue(sut)
        assertEquals(defaultValue, actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changed to true")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changed to true`(
        name: String,
        preferenceKey: String,
        defaultValue: Boolean,
        getValue: (GeneralSettingsHelper) -> Boolean,
    ) {
        sharedPreferences.edit().putBoolean(preferenceKey, true).commit()
        val sut = GeneralSettingsHelper(context)
        val actual = getValue(sut)
        assertTrue(actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changed to false")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changed to false`(
        name: String,
        preferenceKey: String,
        defaultValue: Boolean,
        getValue: (GeneralSettingsHelper) -> Boolean,
    ) {
        sharedPreferences.edit().putBoolean(preferenceKey, false).commit()
        val sut = GeneralSettingsHelper(context)
        val actual = getValue(sut)
        assertFalse(actual)
    }

    @ParameterizedTest(name = "has \"{0}\" the correct value when changing values")
    @MethodSource("testDataForBooleanSettings")
    fun `has boolean settings the correct value when changing values`(
        name: String,
        preferenceKey: String,
        defaultValue: Boolean,
        getValue: (GeneralSettingsHelper) -> Boolean,
    ) {
        val sut = GeneralSettingsHelper(context)
        sharedPreferences.edit().putBoolean(preferenceKey, false).commit()
        assertFalse(getValue(sut))
        sharedPreferences.edit().putBoolean(preferenceKey, true).commit()
        assertTrue(getValue(sut))
    }
}