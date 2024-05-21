package de.marmaro.krt.ffupdater.network.website

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import io.github.g00fy2.versioncompare.Version
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Keep
object MozillaArchiveConsumer {
    private const val BASE_URL = "https://archive.mozilla.org/pub/"

    suspend fun findLatestVersion(fullUrl: String, versionRegex: Regex, cacheBehaviour: CacheBehaviour): String {
        assert(fullUrl.startsWith(BASE_URL))

        val versions = findAllVersions(fullUrl, cacheBehaviour).filter { versionRegex.matches(it) }

        val latestVersion = versions
                .map { Version(it) }
                .maxWith { a, b -> if (a.isHigherThan(b)) 1 else if (a.isEqual(b)) 0 else -1 }

        return requireNotNull(latestVersion.originalString)
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

        val parser = DateTimeFormatter.ofPattern("dd-MMM-u HH:mm")
        val dateTime = LocalDateTime.parse(lastModified, parser)
        return ZonedDateTime.of(dateTime, ZoneId.of("UTC"))
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