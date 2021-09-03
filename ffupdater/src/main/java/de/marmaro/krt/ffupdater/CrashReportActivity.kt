package de.marmaro.krt.ffupdater

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CrashReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crash_report_layout)

        findViewById<Button>(R.id.crash_report__copy_error_message_to_clipboard_button).setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val crashReport = getCrashReport()
            val clip = ClipData.newPlainText("FFUpdater crash report", crashReport)
            clipboardManager.setPrimaryClip(clip)

            Toast.makeText(this, "Crash report is copied to your clipboard", Toast.LENGTH_LONG)
                .show()
        }
        findViewById<Button>(R.id.crash_report__got_to_notabug_org_button).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, NOTABUG_URI))
        }
        findViewById<Button>(R.id.crash_report__got_to_github_button).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URI))
        }
        findViewById<Button>(R.id.crash_report__got_to_gitlab_button).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, GITLAB_URI))
        }

        findViewById<TextView>(R.id.crash_report__exception_class_name).text =
            intent.extras?.getString(EXTRA_EXCEPTION_CLASS_NAME) ?: "/"
        findViewById<TextView>(R.id.crash_report__exception_message).text =
            intent.extras?.getString(EXTRA_EXCEPTION_MESSAGE) ?: "/"
        findViewById<TextView>(R.id.crash_report__exception_stack_trace).text =
            intent.extras?.getString(EXTRA_EXCEPTION_STACK_TRACE) ?: "/"
    }

    private fun getCrashReport(): String {
        val className = intent.extras?.getString(EXTRA_EXCEPTION_CLASS_NAME) ?: "/"
        val message = intent.extras?.getString(EXTRA_EXCEPTION_MESSAGE) ?: "/"
        val stackTrace = intent.extras?.getString(EXTRA_EXCEPTION_STACK_TRACE) ?: "/"
        return "$className\n-\n$message\n-\n$stackTrace"
    }

    companion object {
        const val EXTRA_EXCEPTION_CLASS_NAME = "exception_class_name"
        const val EXTRA_EXCEPTION_MESSAGE = "exception_message"
        const val EXTRA_EXCEPTION_STACK_TRACE = "exception_stack_trace"
        val NOTABUG_URI = Uri.parse("https://notabug.org/Tobiwan/ffupdater/issues")
        val GITHUB_URI = Uri.parse("https://github.com/Tobi823/ffupdater/issues")
        val GITLAB_URI = Uri.parse("https://gitlab.com/Tobiwan/ffupdater_gitlab/-/issues")

        fun createIntentForDisplayingThrowable(context: Context, throwable: Throwable): Intent {
            val intent = Intent(context, CrashReportActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(EXTRA_EXCEPTION_CLASS_NAME, throwable.javaClass.canonicalName)
            intent.putExtra(EXTRA_EXCEPTION_MESSAGE, throwable.localizedMessage)
            intent.putExtra(EXTRA_EXCEPTION_STACK_TRACE, throwable.stackTraceToString())
            return intent
        }
    }
}