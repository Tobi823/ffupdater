package de.marmaro.krt.ffupdater.network.github

import com.google.gson.stream.JsonReader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GithubReleaseJsonConsumerTest {

    @Test
    fun `get the first asset of the first release of the latest 30 Brave releases`() {
        val jsonText =
            javaClass.classLoader!!.getResourceAsStream("de/marmaro/krt/ffupdater/network/github/GithubReleaseJsonConsumer.json")
        val consumer = GithubReleaseJsonConsumer(
            JsonReader(jsonText.bufferedReader()),
            { release -> release.name == "Nightly v1.55.92 (Chromium 115.0.5790.56) \n\n" },
            { asset -> asset.name == "brave-browser-nightly-1.55.92-1.aarch64.rpm" })
        val result = runBlocking { consumer.parseReleaseArrayJson()!! }

        assertEquals("v1.55.92", result.tagName)
        assertEquals(
            "https://github.com/brave/brave-browser/releases/download/v1.55.92/brave-browser-nightly-1.55.92-1.aarch64.rpm",
            result.url
        )
        assertEquals("2023-07-03T13:42:43Z", result.releaseDate)
        assertEquals(104792336, result.fileSizeBytes)
    }

    @Test
    fun `get the last asset of the last release of the latest 30 Brave releases`() {
        val jsonText =
            javaClass.classLoader!!.getResourceAsStream("de/marmaro/krt/ffupdater/network/github/GithubReleaseJsonConsumer.json")
        val consumer = GithubReleaseJsonConsumer(
            JsonReader(jsonText.bufferedReader()),
            { release -> release.name == "Nightly v1.55.10 (Chromium 115.0.5790.40) \n\n" },
            { asset -> asset.name == "policy_templates.zip.sha256.asc" })
        val result = runBlocking { consumer.parseReleaseArrayJson()!! }

        assertEquals("v1.55.10", result.tagName)
        assertEquals(
            "https://github.com/brave/brave-browser/releases/download/v1.55.10/policy_templates.zip.sha256.asc",
            result.url
        )
        assertEquals("2023-06-23T18:09:24Z", result.releaseDate)
        assertEquals(886, result.fileSizeBytes)
    }

    @Test
    fun `get the first asset of the latest release of Brave`() {
        val jsonText =
            javaClass.classLoader!!.getResourceAsStream("de/marmaro/krt/ffupdater/network/github/GithubReleaseJsonConsumer2.json")
        val consumer = GithubReleaseJsonConsumer(
            JsonReader(jsonText.bufferedReader()),
            { release -> release.name == "Release v1.52.129 (Chromium 114.0.5735.198) \n\n" },
            { asset -> asset.name == "brave-browser-1.52.129-1.aarch64.rpm" })
        val result = runBlocking { consumer.parseReleaseJson()!! }

        assertEquals("v1.52.129", result.tagName)
        assertEquals(
            "https://github.com/brave/brave-browser/releases/download/v1.52.129/brave-browser-1.52.129-1.aarch64.rpm",
            result.url
        )
        assertEquals("2023-06-27T20:14:10Z", result.releaseDate)
        assertEquals(103425532, result.fileSizeBytes)
    }

    @Test
    fun `get the last asset of the latest release of Brave`() {
        val jsonText =
            javaClass.classLoader!!.getResourceAsStream("de/marmaro/krt/ffupdater/network/github/GithubReleaseJsonConsumer2.json")
        val consumer = GithubReleaseJsonConsumer(
            JsonReader(jsonText.bufferedReader()),
            { release -> release.name == "Release v1.52.129 (Chromium 114.0.5735.198) \n\n" },
            { asset -> asset.name == "policy_templates.zip.sha256.asc" })
        val result = runBlocking { consumer.parseReleaseJson()!! }

        assertEquals("v1.52.129", result.tagName)
        assertEquals(
            "https://github.com/brave/brave-browser/releases/download/v1.52.129/policy_templates.zip.sha256.asc",
            result.url
        )
        assertEquals("2023-06-27T20:14:10Z", result.releaseDate)
        assertEquals(882, result.fileSizeBytes)
    }
}