package de.marmaro.krt.ffupdater.network.file

import androidx.annotation.Keep

@Keep
data class DownloadStatus(val progressInPercent: Int?, val totalMB: Long)