package de.marmaro.krt.ffupdater.network.fdroid

import com.google.gson.Gson
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer.AppInfo
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer.VersionCodeAndDownloadUrl
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class FdroidConsumerIT {

    @MockK
    lateinit var apiConsumer: ApiConsumer

    @BeforeEach
    fun setUp() {
        prepareApiResponse()
    }

    companion object {
        private const val PACKAGE_NAME = ""
        private const val API_URL = "https://f-droid.org/api/v1/packages/us.spotco.fennec_dos"
        private const val DOWNLOAD_URL = "https://f-droid.org/repo/us.spotco.fennec_dos_"
    }

    private fun prepareApiResponse() {
        val apiResponse = """
            {
              "packageName": "us.spotco.fennec_dos",
              "suggestedVersionCode": 21011120,
              "packages": [
                {
                  "versionName": "101.1.1",
                  "versionCode": 21011120
                },
                {
                  "versionName": "101.1.1",
                  "versionCode": 21011100
                },
                {
                  "versionName": "100.3.0",
                  "versionCode": 21003020
                },
                {
                  "versionName": "100.3.0",
                  "versionCode": 21003000
                }
              ]
            }
        """.trimIndent()
        coEvery {
            apiConsumer.consumeAsync(API_URL, AppInfo::class).await()
        } returns Gson().fromJson(apiResponse, AppInfo::class.java)
    }

    @Test
    fun `get version codes and download urls from api call`() {
        val fdroidConsumer = FdroidConsumer(apiConsumer)
        val result = runBlocking {
            fdroidConsumer.getLatestUpdate("us.spotco.fennec_dos")
        }
        assertEquals("101.1.1", result.versionName)
        assertEquals(2, result.versionCodesAndDownloadUrls.size)

        assertTrue(
            VersionCodeAndDownloadUrl(
                21011120,
                "https://f-droid.org/repo/us.spotco.fennec_dos_21011120.apk"
            ) in result.versionCodesAndDownloadUrls
        )
        assertTrue(
            VersionCodeAndDownloadUrl(
                21011100,
                "https://f-droid.org/repo/us.spotco.fennec_dos_21011100.apk"
            ) in result.versionCodesAndDownloadUrls
        )
    }
}