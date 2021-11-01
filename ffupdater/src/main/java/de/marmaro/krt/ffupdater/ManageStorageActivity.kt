package de.marmaro.krt.ffupdater

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import de.marmaro.krt.ffupdater.crash.CrashListener

class ManageStorageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashListener.openCrashReporterForUncaughtExceptions(this)
        setContentView(R.layout.manage_storage_activity)

        findViewById<Button>(R.id.manage_storage_activity__delete_cached_apk_files).setOnClickListener {
            val folder = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            checkNotNull(folder) { "External download folder should exists." }
            val files = folder.listFiles()
            checkNotNull(files) { "Array of files in download folder should exists." }
            files.forEach {
                check(it.delete()) { "Fail to delete file '${it.name}'." }
            }
            finish()
        }
    }
}