package de.marmaro.krt.ffupdater.installer

import de.marmaro.krt.ffupdater.FFUpdaterException
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ApkChecker {

    companion object {
        suspend fun throwIfApkFileIsNoValidZipFile(file: File) {
            require(file.extension == "apk")
            require(file.exists())
            try {
                withContext(Dispatchers.IO) {
                    ZipFile(file)
                }
            } catch (e: ZipException) {
                throw FFUpdaterException("Downloaded or extracted APK file is not a valid ZIP file.")
            }
        }

        fun throwIfDownloadedFileHasDifferentSize(file: File, latestUpdate: LatestUpdate) {
            require(file.exists())
            val expectedBytes = latestUpdate.fileSizeBytesOfDownload
            if (expectedBytes != null && expectedBytes != file.length()) {
                val message = "Wrong file was downloaded. It should be $expectedBytes bytes long but " +
                        "actual it was ${file.length()} bytes."
                throw NetworkException(message)
            }
        }
    }
}