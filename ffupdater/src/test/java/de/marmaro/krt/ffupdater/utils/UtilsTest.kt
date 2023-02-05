package de.marmaro.krt.ffupdater.utils

import android.os.Build
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.zip.ZipFile

class UtilsTest {

    @Test
    fun getVersionAndCodenameForApiLevel_withAndroidR_returnCorrectText() {
        assertEquals("11 (R)", AndroidVersionCodes.getVersionForApiLevel(Build.VERSION_CODES.R))
    }

    @Test
    fun test1() {
        val test = ZipFile("/home/hacker/Downloads/F-Droid.apk")
    }
}