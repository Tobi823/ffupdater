package de.marmaro.krt.ffupdater.storage

import android.content.Context
import android.os.Environment
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

@Keep
class DownloadedFileCache(private val app: App) {

    fun getApkFile(context: Context, latestUpdate: LatestUpdate): File {
        val fileName = "${getSanitizedPackageName()}_${getSanitizedVersion(latestUpdate)}.apk"
        return File(getCacheFolder(context), fileName)
    }

    fun isApkFileCached(context: Context, latestUpdate: LatestUpdate): Boolean {
        return getApkFile(context, latestUpdate).exists()
    }

    private fun getZipFile(context: Context): File {
        return File(getCacheFolder(context), "${app.findImpl().packageName}.zip")
    }

    /**
     * The downloader may download an APK file or an ZIP archive.
     */
    fun getApkOrZipTargetFileForDownload(context: Context, latestUpdate: LatestUpdate): File {
        if (app.findImpl().isAppPublishedAsZipArchive()) {
            return getZipFile(context)
        }
        return getApkFile(context, latestUpdate)
    }

    fun getCacheFolder(context: Context): File {
        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return checkNotNull(downloadFolder) { "The external 'Download' folder of the app should exists." }
    }

    fun deleteAllApkFileForThisApp(context: Context) {
        getCacheFolder(context)
            .listFiles()
            ?.filter { it.name.startsWith(getSanitizedPackageName()) }
            ?.filter { it.name.endsWith(".apk") }
            ?.forEach { it.delete() }
    }

    fun deleteAllExceptLatestApkFile(context: Context, latestUpdate: LatestUpdate) {
        val latest = getApkFile(context, latestUpdate)
        getCacheFolder(context)
            .listFiles()
            ?.filter { it != latest }
            ?.filter { it.name.startsWith(getSanitizedPackageName()) }
            ?.filter { it.name.endsWith(".apk") }
            ?.forEach { it.delete() }
    }

    fun deleteZipFile(context: Context) {
        require(app.findImpl().isAppPublishedAsZipArchive())
        val file = getZipFile(context)
        if (file.exists()) {
            val success = file.delete()
            check(success) { "Fail to delete file '${file.absolutePath}'." }
        }
    }

    suspend fun extractApkFromZipArchive(context: Context, latestUpdate: LatestUpdate) {
        require(app.findImpl().isAppPublishedAsZipArchive())

        val zipArchive = getZipFile(context)
        require(zipArchive.exists())
        val apkFile = getApkFile(context, latestUpdate)
        apkFile.delete()

        withContext(Dispatchers.IO) {
            ZipFile(zipArchive).use { zipFile ->
                internalExtractApkFromZipArchive(zipFile, apkFile)
            }
        }
    }

    private fun internalExtractApkFromZipArchive(zipFile: ZipFile, apkFile: File) {
        requireNotNull(app.findImpl().fileNameInZipArchive)

        val zipEntries = zipFile.entries().toList()
        val apkEntry = zipEntries.firstOrNull { it.name == app.findImpl().fileNameInZipArchive }
            ?: throw RuntimeException("Missing APK in ZIP. It contains: ${zipEntries.map { it.name }}")

        zipFile.getInputStream(apkEntry).buffered().use { apkZipEntryStream ->
            apkFile.outputStream().buffered().use { apkFileStream ->
                apkZipEntryStream.copyTo(apkFileStream)
                apkFileStream.flush()
            }
        }
    }

    private fun getSanitizedPackageName(): String {
        return app.findImpl().packageName.replace("""\W""".toRegex(), "_")
    }

    private fun getSanitizedVersion(latestUpdate: LatestUpdate): String {
        return latestUpdate.version.replace("""\W""".toRegex(), "_")
    }
}