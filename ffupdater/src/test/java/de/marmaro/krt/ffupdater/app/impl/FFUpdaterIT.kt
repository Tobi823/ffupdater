package de.marmaro.krt.ffupdater.app.impl

import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.FileDownloader.CacheBehaviour.FORCE_NETWORK
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class FFUpdaterIT : BaseAppIT() {

    @Test
    fun findAppUpdateStatus() {
        val ffupdater = FFUpdater(GithubConsumer.INSTANCE)
        val fileDownloader = FileDownloader(NetworkSettingsHelper(context), context, FORCE_NETWORK)
        val result = runBlocking { ffupdater.findLatestUpdate(context, fileDownloader) }
        requireNotNull(result)
        verifyThatDownloadLinkAvailable(result.downloadUrl)
    }
}