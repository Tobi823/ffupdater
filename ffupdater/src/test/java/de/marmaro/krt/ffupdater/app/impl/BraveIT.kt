package de.marmaro.krt.ffupdater.app.impl

import de.marmaro.krt.ffupdater.network.github.GithubConsumer
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
internal class BraveIT : BaseAppIT() {

    @Test
    fun checkForUpdateWithoutLoadingFromCacheAsync() {
        every { deviceSdkTester.supportsAndroidNougat() } returns true
        val brave = Brave(GithubConsumer.INSTANCE, deviceAbiExtractor, deviceSdkTester)
        val result = runBlocking { brave.findLatestUpdate(context) }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        val releaseDate = ZonedDateTime.parse(result.publishDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val age = Duration.between(releaseDate, ZonedDateTime.now())
        assertTrue(age.toDays() < 4 * 7) { "${age.toDays()} days is too old" }
        // don't check for firstReleaseHasAssets because it is common that some releases has no APK files
    }
}