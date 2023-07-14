package de.marmaro.krt.ffupdater.utils

import android.os.Build
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UtilsTest {

    @Test
    fun getVersionAndCodenameForApiLevel_withAndroidR_returnCorrectText() {
        assertEquals("11 (R)", AndroidVersionCodes.getVersionForApiLevel(Build.VERSION_CODES.R))
    }
}