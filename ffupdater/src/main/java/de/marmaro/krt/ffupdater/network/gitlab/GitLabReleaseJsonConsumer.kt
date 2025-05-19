package de.marmaro.krt.ffupdater.network.gitlab

import androidx.annotation.Keep
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.function.Predicate

@Keep
class GitLabReleaseJsonConsumer(
    private val reader: JsonReader,
    private val correctRelease: Predicate<GitLabConsumer.SearchParameterForRelease>,
    private val correctAsset: Predicate<GitLabConsumer.SearchParameterForAsset>,
    private val requireReleaseDescription: Boolean,
) {

    suspend fun parseReleaseArrayJson(): GitLabConsumer.Result? =
        withContext(Dispatchers.Default) { handleReleaseArray() }

    suspend fun parseReleaseJson(): GitLabConsumer.Result? =
        withContext(Dispatchers.Default) { handleReleaseObject() }

    private fun handleReleaseArray(): GitLabConsumer.Result? {
        reader.beginArray()
        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            handleReleaseObject()?.let { return it }
        }
        reader.endArray()
        return null
    }

    private fun handleReleaseObject(): GitLabConsumer.Result? {
        reader.beginObject()
        val currentRelease = CurrentRelease()
        var foundAsset: FoundAsset? = null

        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "tag_name" -> currentRelease.tagName = reader.nextString()
                "created_at" -> currentRelease.createdAt = reader.nextString()
                "name" -> currentRelease.name = reader.nextString()
                "description" -> if (requireReleaseDescription) currentRelease.description = reader.nextString() else skipNextJsonEntry(reader)
                "assets" -> {

                    reader.beginObject()
                    while (reader.peek() == JsonToken.NAME) {
                        if (reader.nextName() == "links") {
                            reader.beginArray()
                            while (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                handleAssetObject()?.let { if (foundAsset == null && correctAsset.test(it.toSearchParameterForAsset())) foundAsset = it }
                            }
                            reader.endArray()
                        } else {
                            skipNextJsonEntry(reader)
                        }
                    }
                    reader.endObject()
                }
                else -> skipNextJsonEntry(reader)
            }
        }
        reader.endObject()

        return if (currentRelease.tagName != null &&
            correctRelease.test(currentRelease.toSearchParameterForRelease()) &&
            foundAsset != null &&
            (!requireReleaseDescription || currentRelease.description != null)
        ) {
            GitLabConsumer.Result(
                tagName = currentRelease.tagName!!,
                url = foundAsset!!.downloadUrl,
                fileSizeBytes = foundAsset!!.size,
                releaseDate = currentRelease.createdAt ?: "",
                releaseDescription = currentRelease.description
            )
        } else null
    }

    private fun handleAssetObject(): FoundAsset? {
        reader.beginObject()
        var name: String? = null
        var size: Long? = null
        var url: String? = null
        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "name" -> name = reader.nextString()
                "url" -> url = reader.nextString()
                "size" -> size = try { reader.nextLong() } catch (_: Throwable) { null }
                else -> skipNextJsonEntry(reader)
            }
        }
        reader.endObject()
        return if (name != null && url != null) {
            FoundAsset(name, size ?: 0L, url)
        } else null
    }

    private fun skipNextJsonEntry(reader: JsonReader) {
        when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.peek() != JsonToken.END_ARRAY) skipNextJsonEntry(reader)
                reader.endArray()
            }
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                while (reader.peek() != JsonToken.END_OBJECT) skipNextJsonEntry(reader)
                reader.endObject()
            }
            JsonToken.STRING -> reader.nextString()
            JsonToken.NUMBER -> reader.nextDouble()
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NULL -> reader.nextNull()
            else -> reader.skipValue()
        }
    }

    @Keep
    private data class CurrentRelease(
        var name: String? = null,
        var tagName: String? = null,
        var createdAt: String? = null,
        var description: String? = null,
    ) {
        fun toSearchParameterForRelease() =
            GitLabConsumer.SearchParameterForRelease(name ?: "", false)
    }

    @Keep
    private data class FoundAsset(
        val name: String,
        val size: Long,
        val downloadUrl: String,
    ) {
        fun toSearchParameterForAsset() = GitLabConsumer.SearchParameterForAsset(name)
    }
}
