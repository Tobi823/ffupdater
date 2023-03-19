package de.marmaro.krt.ffupdater.app.impl

import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.FileDownloader.CacheBehaviour.FORCE_NETWORK
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import io.mockk.every
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ExtendWith(MockKExtension::class)
internal class BraveBetaIT : BaseAppIT() {

    @Test
    fun checkForUpdateWithoutLoadingFromCacheAsync() {
        every { deviceSdkTester.supportsAndroidNougat() } returns true
        val brave = BraveBeta(GithubConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester)
        val fileDownloader = FileDownloader(NetworkSettingsHelper(context), context, FORCE_NETWORK)
        val result = runBlocking { brave.findLatestUpdate(context, fileDownloader) }
        requireNotNull(result)
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(
            result.publishDate,
            DateTimeFormatter.ISO_ZONED_DATE_TIME
        )
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 2 * 7) { "${age.toDays()} days is too old" }
        // don't check for firstReleaseHasAssets because it is common that some releases has no APK files
    }
}