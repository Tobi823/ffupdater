package de.marmaro.krt.ffupdater.installer

import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.installer.exceptions.InvalidApkException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ApkChecker {

    companion object {
        suspend fun throwIfApkFileIsNoValidZipFile(file: File) {
            if (file.extension != "apk") throw InvalidApkException("Wrong file downloaded: $file")
            if (!file.exists()) throw InvalidApkException("Missing file: $file")

            try {
                withContext(Dispatchers.IO) {
                    ZipFile(file)
                }
            } catch (e: ZipException) {
                throw InvalidApkException("Downloaded or extracted APK file is not a valid ZIP file.", e)
            }
        }

        fun throwIfDownloadedFileHasDifferentSize(file: File, latestUpdate: LatestUpdate) {
            if (!file.exists()) throw NetworkException("File was not downloaded: $file")

            val expectedBytes = latestUpdate.exactFileSizeBytesOfDownload
            if (expectedBytes != null && expectedBytes != file.length()) {
                val message = "Wrong file was downloaded. It should be $expectedBytes bytes long but " +
                        "actual it was ${file.length()} bytes."
                throw NetworkException(message)
            }
        }
    }
}