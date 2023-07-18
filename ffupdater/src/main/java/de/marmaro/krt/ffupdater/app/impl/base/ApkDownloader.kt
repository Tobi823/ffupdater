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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

@Keep
interface ApkDownloader : AppAttributes {

    suspend fun <R> download(
        context: Context,
        latestVersion: LatestVersion,
        progress: suspend (Deferred<Any>, Channel<DownloadStatus>) -> R,
    ) {
        val downloadFile = getDownloadFile(context.applicationContext)
        downloadFile.delete()
        try {
            download(context, latestVersion, downloadFile, progress)
        } finally {
            downloadFile.delete()
        }
    }

    fun isApkDownloaded(context: Context, latestVersion: LatestVersion): Boolean {
        return getApkFile(context.applicationContext, latestVersion).exists()
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
            Log.i(LOG_TAG, "ApkDownloader: Delete file cache")
            getApkCacheFolder(context.applicationContext).listFiles()!!
                .filter { it.name.startsWith("${getSanitizedPackageName()}_") }
                .filter { it.name.endsWith(".apk") }
                .forEach { it.delete() }
        }
    }

    suspend fun deleteFileCacheExceptLatest(context: Context, latestVersion: LatestVersion) {
        withContext(Dispatchers.IO) {
            Log.i(LOG_TAG, "ApkDownloader: Delete file cache except latest")
            val latestFile = getApkFile(context.applicationContext, latestVersion)
            getApkCacheFolder(context.applicationContext).listFiles()!!
                .filter { it != latestFile }
                .filter { it.name.startsWith("${getSanitizedPackageName()}_") }
                .filter { it.name.endsWith(".apk") }
                .forEach { it.delete() }
        }
    }

    private fun getDownloadFile(context: Context): File {
        val cacheFolder = getApkCacheFolder(context.applicationContext)
        return File(cacheFolder, "${getSanitizedPackageName()}_download.download")
    }

    private fun getSanitizedPackageName(): String {
        return packageName.replace("""\W""".toRegex(), "_")
    }

    private fun getSanitizedVersion(latestVersion: LatestVersion): String {
        return latestVersion.version.replace("""\W""".toRegex(), "_")
    }

    private suspend fun <R> download(
        context: Context,
        latestVersion: LatestVersion,
        downloadFile: File,
        progress: suspend (Deferred<Any>, Channel<DownloadStatus>) -> R,
    ) {
        val (deferred, progressChannel) = FileDownloader.downloadFile(latestVersion.downloadUrl, downloadFile)
        withContext(Dispatchers.Main) {
            progress(deferred, progressChannel)
        }
        deferred.await()
        checkDownloadFile(downloadFile, latestVersion)
        processDownload(context.applicationContext, downloadFile, latestVersion)
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
            processApkDownload(downloadFile, apkFile)
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

    private fun processApkDownload(downloadFile: File, apkFile: File) {
        downloadFile.renameTo(apkFile)
    }

    private suspend fun checkApkFile(file: File) {
        if (file.extension != "apk") throw InvalidApkException("Wrong file downloaded: $file")
        if (!file.exists()) throw InvalidApkException("Missing file: $file")
        require(StorageUtil.isValidZipFile(file)) { "Downloaded or extracted APK file is not a valid ZIP file." }
    }

}