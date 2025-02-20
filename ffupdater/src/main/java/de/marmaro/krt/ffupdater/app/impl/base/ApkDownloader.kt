package de.marmaro.krt.ffupdater.app.impl.base

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.installer.exceptions.InvalidApkException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.storage.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipFile

@Keep
interface ApkDownloader : AppAttributes {

    suspend fun download(
        context: Context,
        latestVersion: LatestVersion,
        progress: Channel<DownloadStatus>,
    ) {
        val temp = generateTempApkFile(context.applicationContext) // temp file only for APK analysis
        try {
            temp.delete()
            FileDownloader.downloadFile(latestVersion.downloadUrl, temp, progress)
            checkDownloadFile(temp, latestVersion)
            processDownload(context.applicationContext, temp, latestVersion)
        } finally {
            temp.delete()
            progress.close(RuntimeException("Progress channel was not yet closed. This should never happen"))
        }
    }

    suspend fun isApkDownloaded(context: Context, latestVersion: LatestVersion): Boolean {
        val file = getApkFile(context.applicationContext, latestVersion)
        return file.exists() && StorageUtil.isValidZipOrApkFile(file)
    }

    fun getApkCacheFolder(context: Context): File {
        return context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }

    fun getApkFile(context: Context, latestVersion: LatestVersion): File {
        val cacheFolder = getApkCacheFolder(context.applicationContext)
        return File(cacheFolder, "${getSanitizedPackageName()}_${getSanitizedVersion(latestVersion)}.apk")
    }

    suspend fun deleteFileCache(context: Context) {
        withContext(Dispatchers.IO) {
            getApkCacheFolder(context.applicationContext).listFiles()!!
                .filter { it.name.startsWith("${getSanitizedPackageName()}_") }.filter { it.name.endsWith(".apk") }
                .forEach {
                    Log.d(LOG_TAG, "ApkDownloader: Delete cached APK of ${app.name} (${it.absolutePath}).")
                    it.delete()
                }
        }
    }

    suspend fun deleteFileCacheExceptLatest(context: Context, latestVersion: LatestVersion) {
        withContext(Dispatchers.IO) {
            val latestFile = getApkFile(context.applicationContext, latestVersion)
            getApkCacheFolder(context.applicationContext).listFiles()!!.filter { it != latestFile }
                .filter { it.name.startsWith("${getSanitizedPackageName()}_") }.filter { it.name.endsWith(".apk") }
                .also { if (it.isNotEmpty()) Log.i(LOG_TAG, "ApkDownloader: Delete older files from ${app.name}") }
                .forEach { it.delete() }
        }
    }

    private fun generateTempApkFile(context: Context): File {
        val cacheFolder = getApkCacheFolder(context.applicationContext)
        val suffix = if (isAppPublishedAsZipArchive()) ".zip" else ".apk"
        return File(cacheFolder, UUID.randomUUID().toString() + suffix)
    }

    private fun getSanitizedPackageName(): String {
        return packageName.replace("""\W""".toRegex(), "_")
    }

    private fun getSanitizedVersion(latestVersion: LatestVersion): String {
        val version = latestVersion.version
        val sanitizedVersionText = version.versionText.replace("""\W""".toRegex(), "_")
        val dateOrEmptyString = version.buildDate?.format(DateTimeFormatter.BASIC_ISO_DATE)?.let { "_$it" } ?: ""
        return sanitizedVersionText + dateOrEmptyString
    }

    private suspend fun checkDownloadFile(file: File, latestVersion: LatestVersion) {
        withContext(Dispatchers.IO) {
            if (!file.exists()) throw NetworkException("File was not downloaded: $file")
            val expectedBytes = latestVersion.exactFileSizeBytesOfDownload
            if (expectedBytes != null && expectedBytes != file.length()) {
                throw NetworkException("Size of download should be $expectedBytes bytes, but it is ${file.length()} bytes.")
            }
        }
    }

    private suspend fun processDownload(
        context: Context,
        downloadFile: File,
        latestVersion: LatestVersion,
    ) {
        val apkFile = getApkFile(context.applicationContext, latestVersion)
        apkFile.delete()
        if (isAppPublishedAsZipArchive()) {
            processZipDownload(downloadFile, apkFile)
        } else {
            downloadFile.renameTo(apkFile)
        }
        checkApkFile(apkFile)
    }

    private fun isAppPublishedAsZipArchive() = (fileNameInZipArchive != null)

    private suspend fun processZipDownload(downloadFile: File, apkFile: File) {
        withContext(Dispatchers.IO) {
            ZipFile(downloadFile).use { zip ->
                val apkEntry = zip.entries().toList().first { it.name == fileNameInZipArchive }
                zip.getInputStream(apkEntry).buffered().use { zipStream ->
                    apkFile.outputStream().buffered().use { apkStream ->
                        zipStream.copyTo(apkStream)
                    }
                }
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun checkApkFile(file: File) {
        if (file.extension != "apk") throw InvalidApkException("Wrong file downloaded: $file")
        if (!file.exists()) throw InvalidApkException("Missing file: $file")
        require(StorageUtil.isValidZipOrApkFile(file)) { "Downloaded or extracted APK file is not a valid ZIP file." }
    }

}