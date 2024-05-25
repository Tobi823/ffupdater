package de.marmaro.krt.ffupdater.network.website

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.VersionCompareHelper
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Keep
object MozillaArchiveConsumer {
    private const val BASE_URL = "https://archive.mozilla.org/pub/"
    private val MONTH_NAME_TO_MONTH_NUMBER = mapOf(
        "January" to "01",
        "February" to "02",
        "March" to "03",
        "April" to "04",
        "May" to "05",
        "June" to "06",
        "July" to "07",
        "August" to "08",
        "September" to "09",
        "October" to "10",
        "November" to "11",
        "December" to "12"
    )

    suspend fun findLatestVersion(fullUrl: String, versionRegex: Regex, cacheBehaviour: CacheBehaviour): String {
        assert(fullUrl.startsWith(BASE_URL))

        val versions = findAllVersions(fullUrl, cacheBehaviour).filter { versionRegex.matches(it) }

        return versions.maxWith { a, b ->
            when {
                VersionCompareHelper.isAvailableVersionHigher(b, a) -> 1
                VersionCompareHelper.isAvailableVersionEqual(b, a) -> 0
                else -> -1
            }
        }
    }

    private suspend fun findAllVersions(fullUrl: String, cacheBehaviour: CacheBehaviour): List<String> {
        val webpage = FileDownloader.downloadStringWithCache(fullUrl, cacheBehaviour)
        val regex = Regex("""<a href="[a-z0-9.\-_/]+">([a-z0-9.\-_]+)/</a>""")
        val allResults = regex.findAll(webpage)
        val versionStrings = allResults
                .toList()
                .map { it.groups[1]?.value ?: "" }
        return versionStrings
    }

    suspend fun findDateTimeFromPage(page: String, cacheBehaviour: CacheBehaviour): ZonedDateTime {
        val webpage = FileDownloader.downloadStringWithCache(page, cacheBehaviour)
        val regex = Regex("""<td>((\d{1,2}+)-(\w+)-(\d{4}) (\d{1,2}):(\d{1,2}))</td>""")
        val lastModified = regex.find(webpage)?.groups?.get(1)?.value
        requireNotNull(lastModified) { "unable to extract timestamp: $webpage" }
        return parseTimestamp(lastModified)
    }

    private fun parseTimestamp(timestamp: String): ZonedDateTime {
        for ((monthName, monthValue) in MONTH_NAME_TO_MONTH_NUMBER) {
            if (monthName in timestamp) {
                val improvedTimestamp = timestamp.replace(monthName, monthValue)
                val parser = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")
                val dateTime = LocalDateTime.parse(improvedTimestamp, parser)
                return ZonedDateTime.of(dateTime, ZoneId.of("UTC"))
            }
        }
        throw RuntimeException("invalid timestamp: $timestamp")
    }

    suspend fun findLastLink(page: String, cacheBehaviour: CacheBehaviour): String {
        val webpage = FileDownloader.downloadStringWithCache(page, cacheBehaviour)
        val regex = Regex("""<a href="([a-z0-9.\-_/]+)">""")
        val links = regex
                .findAll(webpage)
                .toList()
        require(links.isNotEmpty())
        val lastGroup = links.last().groups[1]
        requireNotNull(lastGroup)
        return lastGroup.value
    }
}