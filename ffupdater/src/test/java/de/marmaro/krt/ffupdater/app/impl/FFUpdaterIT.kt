package de.marmaro.krt.ffupdater.app.impl

import com.google.gson.Gson
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import io.mockk.coEvery
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.FileReader

@ExtendWith(MockKExtension::class)
class FFUpdaterIT : BaseAppIT() {
    @BeforeEach
    fun setUp() {
        setUp(App.FFUPDATER)

        val path = "src/test/resources/de/marmaro/krt/ffupdater/app/impl/FFUpdater/latest.json"
        coEvery {
            val url = "$API_URL/latest"
            apiConsumer.consumeAsync(url, GithubConsumer.Release::class).await()
        } returns Gson().fromJson(FileReader(path), GithubConsumer.Release::class.java)
    }

    companion object {
        private const val API_URL = "https://api.github.com/repos/Tobi823/ffupdater/releases"
        private const val DOWNLOAD_URL = "https://github.com/Tobi823/ffupdater/releases/download"
    }

    private fun createSut(): FFUpdater {
        return FFUpdater(apiConsumer = apiConsumer)
    }

    @Test
    fun `check download info`() {
        val result = runBlocking { createSut().checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        assertEquals("$DOWNLOAD_URL/75.1.0/ffupdater-release.apk", result.downloadUrl)
        assertEquals("75.1.0", result.version)
        assertEquals(3151577L, result.fileSizeBytes)
        assertEquals("2022-04-08T16:47:27Z", result.publishDate)
    }

    @Test
    fun `update check - outdated version installed`() {
        packageInfo.versionName = "75.0.2"
        val result = runBlocking { createSut().checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        assertTrue(result.isUpdateAvailable)
    }

    @Test
    fun `update check - latest version installed`() {
        packageInfo.versionName = "75.1.0"
        val result = runBlocking { createSut().checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        assertFalse(result.isUpdateAvailable)
    }
}
