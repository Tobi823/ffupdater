package de.marmaro.krt.ffupdater.installer

import android.content.Context
import android.content.Intent
import de.marmaro.krt.ffupdater.download.DownloadManagerAdapter

interface AppInstaller {
    fun onNewIntentCallback(intent: Intent, context: Context)
    fun install(context: Context,
                downloadManagerAdapter: DownloadManagerAdapter,
                downloadId: Long)
}