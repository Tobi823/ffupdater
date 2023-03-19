package de.marmaro.krt.ffupdater.app.impl

import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.FileDownloader.CacheBehaviour.FORCE_NETWORK
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class VivaldiIT : BaseAppIT() {

    @Test
    fun findAppUpdateStatus() {
        val vivaldi = Vivaldi(ApiConsumer.INSTANCE, deviceAbiExtractor)
        val fileDownloader = FileDownloader(NetworkSettingsHelper(context), context, FORCE_NETWORK)
        val result = runBlocking { vivaldi.findLatestUpdate(context, fileDownloader) }
        requireNotNull(result)
        verifyThatDownloadLinkAvailable(result.downloadUrl)
        assertFalse(result.version.isEmpty())
    }
}