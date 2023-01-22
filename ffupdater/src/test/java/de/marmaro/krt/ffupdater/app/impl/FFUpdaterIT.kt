package de.marmaro.krt.ffupdater.app.impl

import de.marmaro.krt.ffupdater.network.github.GithubConsumer
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
        val result = runBlocking { ffupdater.findAppUpdateStatus(context) }
        verifyThatDownloadLinkAvailable(result.latestUpdate.downloadUrl)
    }
}