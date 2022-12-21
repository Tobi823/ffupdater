package de.marmaro.krt.ffupdater.app.impl

import de.marmaro.krt.ffupdater.network.ApiConsumer
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class VivaldiIT : BaseAppIT() {

    @Test
    fun checkForUpdateWithoutLoadingFromCacheAsync() {
        val vivaldi = Vivaldi(ApiConsumer.INSTANCE, deviceAbiExtractor)
        val result = runBlocking { vivaldi.checkForUpdateWithoutLoadingFromCacheAsync(context).await() }
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertFalse(result.version.isEmpty())
        assertTrue(result.firstReleaseHasAssets)
    }
}