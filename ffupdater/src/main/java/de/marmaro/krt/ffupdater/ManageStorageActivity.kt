package de.marmaro.krt.ffupdater

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import de.marmaro.krt.ffupdater.crash.CrashListener

class ManageStorageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CrashListener.openCrashReporterForUncaughtExceptions(this)) {
            finish()
            return
        }

        setContentView(R.layout.manage_storage_activity)
        findViewById<Button>(R.id.manage_storage_activity__delete_cached_apk_files).setOnClickListener {
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?.listFiles()
                ?.forEach {
                    check(it.delete()) { "Fail to delete file '${it.name}'." }
                }
                ?: throw IllegalStateException("Folder does not exists.")
            finish()
        }
    }
}