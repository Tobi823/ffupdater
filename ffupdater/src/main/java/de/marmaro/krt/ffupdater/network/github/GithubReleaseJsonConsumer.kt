package de.marmaro.krt.ffupdater.network.github

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import okhttp3.ResponseBody
import java.util.function.Predicate


class GithubReleaseJsonConsumer(
    private val reader: JsonReader,
    private val correctRelease: Predicate<GithubConsumer.SearchParameterForRelease>,
    private val correctAsset: Predicate<GithubConsumer.SearchParameterForAsset>,
) {

    constructor(
        responseBody: ResponseBody,
        correctRelease: Predicate<GithubConsumer.SearchParameterForRelease>,
        correctAsset: Predicate<GithubConsumer.SearchParameterForAsset>,
    ) : this(JsonReader(responseBody.charStream().buffered()), correctRelease, correctAsset)


    fun parseReleaseArrayJson(): GithubConsumer.Result? {
        return handleReleaseArray()
    }

    fun parseReleaseJson(): GithubConsumer.Result? {
        return handleReleaseObject()
    }

    private fun handleReleaseArray(): GithubConsumer.Result? {
        assert(reader.peek() == JsonToken.BEGIN_ARRAY)
        reader.beginArray()

        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            handleReleaseObject()?.let { return it }
        }

        assert(reader.peek() == JsonToken.END_ARRAY)
        reader.endArray()
        return null
    }

    private fun handleReleaseObject(): GithubConsumer.Result? {
        assert(reader.peek() == JsonToken.BEGIN_OBJECT)
        reader.beginObject()

        val currentRelease = CurrentRelease()
        var foundAsset: FoundAsset? = null

        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "tag_name" -> currentRelease.tagName = reader.nextString()
                "published_at" -> currentRelease.publishedAt = reader.nextString()
                "name" -> currentRelease.name = reader.nextString()
                "prerelease" -> currentRelease.prerelease = reader.nextBoolean()
                "assets" -> {
                    if (currentRelease.isDataSetForSearchParameterConversion()) {
                        val release = currentRelease.toSearchParameterForRelease()
                        if (correctRelease.test(release)) {
                            foundAsset = handleAssetArray()
                        } else {
                            // ignore assets if release is not correct
                            skipNextJsonEntry(reader)
                        }
                    } else {
                        // should not happen, because assets comes after name and prerelease
                        foundAsset = handleAssetArray()
                    }
                }

                else -> skipNextJsonEntry(reader)
            }
        }

        if (currentRelease.isDataSetForSearchParameterConversion() &&
            correctRelease.test(currentRelease.toSearchParameterForRelease()) &&
            foundAsset != null &&
            correctAsset.test(foundAsset.toSearchParameterForAsset())
        ) {
            return GithubConsumer.Result(
                tagName = currentRelease.tagName!!,
                url = foundAsset.downloadUrl,
                fileSizeBytes = foundAsset.size,
                releaseDate = currentRelease.publishedAt!!
            )
        }

        assert(reader.peek() == JsonToken.END_OBJECT)
        reader.endObject()
        return null
    }

    private fun handleAssetArray(): FoundAsset? {
        assert(reader.peek() == JsonToken.BEGIN_ARRAY)
        reader.beginArray()

        var foundAsset: FoundAsset? = null

        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            // only store, don't abort JsonReader - maybe we have to read more data from JSON
            handleAssetObject()
                ?.let { foundAsset = it }
        }

        assert(reader.peek() == JsonToken.END_ARRAY)
        reader.endArray()
        return foundAsset
    }

    private fun handleAssetObject(): FoundAsset? {
        assert(reader.peek() == JsonToken.BEGIN_OBJECT)
        reader.beginObject()

        val currentAsset = CurrentAsset()
        while (reader.peek() == JsonToken.NAME) {
            // process asset
            when (reader.nextName()) {
                "name" -> currentAsset.name = reader.nextString()
                "size" -> currentAsset.size = reader.nextLong()
                "browser_download_url" -> currentAsset.downloadUrl = reader.nextString()
                else -> skipNextJsonEntry(reader)
            }
        }

        assert(reader.peek() == JsonToken.END_OBJECT)
        reader.endObject()

        if (currentAsset.isDataSetForSearchParameterConversion() &&
            correctAsset.test(currentAsset.toSearchParameterForAsset())
        ) {
            return FoundAsset(currentAsset)
        }
        return null
    }

    private fun skipNextJsonEntry(reader: JsonReader) {
        when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.peek() != JsonToken.END_ARRAY) {
                    if (reader.peek() == JsonToken.NAME) {
                        reader.nextName()
                    }
                    skipNextJsonEntry(reader)
                }
                reader.endArray()
            }

            JsonToken.END_ARRAY -> {}
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                while (reader.peek() != JsonToken.END_OBJECT) {
                    if (reader.peek() == JsonToken.NAME) {
                        reader.nextName()
                    }
                    skipNextJsonEntry(reader)
                }
                reader.endObject()
            }

            JsonToken.END_OBJECT -> {}
            JsonToken.NAME -> throw IllegalArgumentException("a")
            JsonToken.STRING -> reader.nextString()
            JsonToken.NUMBER -> reader.nextDouble()
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NULL -> reader.nextNull()
            JsonToken.END_DOCUMENT -> {}
            null -> {}
        }

    }


    data class CurrentRelease(
        var name: String? = null,
        var prerelease: Boolean? = null,
        var tagName: String? = null,
        var publishedAt: String? = null,
    ) {
        fun isDataSetForSearchParameterConversion(): Boolean {
            return name != null && prerelease != null
        }

        fun toSearchParameterForRelease(): GithubConsumer.SearchParameterForRelease {
            return GithubConsumer.SearchParameterForRelease(name!!, prerelease!!)
        }
    }


    data class CurrentAsset(
        var name: String? = null,
        var size: Long? = null,
        var downloadUrl: String? = null,
    ) {
        fun isDataSetForSearchParameterConversion(): Boolean {
            return name != null && size != null && downloadUrl != null
        }

        fun toSearchParameterForAsset(): GithubConsumer.SearchParameterForAsset {
            return GithubConsumer.SearchParameterForAsset(name!!)
        }
    }

    data class FoundAsset(
        val name: String,
        val size: Long,
        val downloadUrl: String,
    ) {
        constructor(currentAsset: CurrentAsset) : this(
            currentAsset.name!!, currentAsset.size!!, currentAsset.downloadUrl!!
        )

        fun toSearchParameterForAsset(): GithubConsumer.SearchParameterForAsset {
            return GithubConsumer.SearchParameterForAsset(name)
        }
    }
}