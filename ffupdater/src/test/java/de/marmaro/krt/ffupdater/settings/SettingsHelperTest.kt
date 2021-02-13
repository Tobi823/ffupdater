package de.marmaro.krt.ffupdater.settings

import android.content.Context
import android.content.SharedPreferences
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock


class SettingsHelperTest : TestCase() {

    private var context: Context? = null
    private var sharedPreferences: SharedPreferences? = null

    @Before
    override fun setUp() {
        context = mock(Context::class.java)
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        `when`(context!!.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
    }

    @Test
    fun getAutomaticCheck_withNothing_returnTrue() {
        sharedPreferences!!.edit().putBoolean("automaticCheck", false).commit();
        assertFalse(SettingsHelper(context!!).automaticCheck)
    }

    fun testGetCheckInterval() {}

    fun testGetDisabledApps() {}

    fun testGetThemePreference() {}
}