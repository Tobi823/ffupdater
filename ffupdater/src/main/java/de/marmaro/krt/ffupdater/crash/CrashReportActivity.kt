package de.marmaro.krt.ffupdater.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.BOARD
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.HOST
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import android.os.Build.PRODUCT
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.TAGS
import android.os.Build.TIME
import android.os.Build.USER
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import de.marmaro.krt.ffupdater.BuildConfig.BUILD_TYPE
import de.marmaro.krt.ffupdater.BuildConfig.VERSION_CODE
import de.marmaro.krt.ffupdater.BuildConfig.VERSION_NAME
import de.marmaro.krt.ffupdater.R

@Keep
class CrashReportActivity : AppCompatActivity() {

    private lateinit var explanation: String
    private lateinit var stackTrace: String
    private lateinit var logs: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_report)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        explanation = intent.extras?.getString(EXTRA_EXCEPTION_EXPLANATION, "") ?: ""
        stackTrace = intent.extras?.getString(EXTRA_EXCEPTION_STACK_TRACE, "") ?: ""
        logs = intent.extras?.getString(EXTRA_EXCEPTION_LOGS) ?: ""

        findViewById<TextView>(R.id.crash_report__explanation_textview).text = explanation
        findViewById<TextView>(R.id.crash_report__exception_stack_trace).text = "$explanation\n\n$logs"

        findViewById<Button>(R.id.crash_report__copy_error_message_to_clipboard_button).setOnClickListener {
            copyErrorMessageToClipboard()
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
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun copyErrorMessageToClipboard() {
        val clip = ClipData.newPlainText("FFUpdater crash report", getCrashReport())
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        Toast.makeText(this, "Crash report is copied to your clipboard", Toast.LENGTH_LONG)
            .show()
    }

    private fun getCrashReport(): String {
        return """
            |Stacktrace:
            |```
            |${intent.extras?.getString(EXTRA_EXCEPTION_STACK_TRACE)}
            |```
            |Logs:
            |```
            |${intent.extras?.getString(EXTRA_EXCEPTION_STACK_TRACE)}
            |```
            |Device information:
            || Key | Value |
            || --- | --- |
            || FFUpdater version | $VERSION_NAME ($VERSION_CODE) $BUILD_TYPE |
            || Device | $MODEL ($PRODUCT, $DEVICE, $BOARD) |
            || Manufacturer | $BRAND ($MANUFACTURER) | 
            || Supported ABIs | ${SUPPORTED_ABIS.joinToString()} |
            || Android version | $RELEASE (SDK: $SDK_INT) |
            || OS | $HOST, $USER, $TAGS, $TIME |         
        """.trimMargin()
    }

    companion object {
        const val EXTRA_EXCEPTION_STACK_TRACE = "exception_stack_trace"
        const val EXTRA_EXCEPTION_LOGS = "exception_logs"
        const val EXTRA_EXCEPTION_EXPLANATION = "exception_explanation"
        val NOTABUG_URI: Uri = Uri.parse("https://notabug.org/Tobiwan/ffupdater/issues")
        val GITHUB_URI: Uri = Uri.parse("https://github.com/Tobi823/ffupdater/issues")
        val GITLAB_URI: Uri = Uri.parse("https://gitlab.com/Tobiwan/ffupdater_gitlab/-/issues")


        fun createIntent(context: Context, throwableAndLogs: ThrowableAndLogs, description: String): Intent {
            val intent = Intent(context.applicationContext, CrashReportActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(EXTRA_EXCEPTION_STACK_TRACE, throwableAndLogs.stacktrace)
            intent.putExtra(EXTRA_EXCEPTION_LOGS, throwableAndLogs.logs)
            intent.putExtra(EXTRA_EXCEPTION_EXPLANATION, description)
            return intent
        }
    }
}