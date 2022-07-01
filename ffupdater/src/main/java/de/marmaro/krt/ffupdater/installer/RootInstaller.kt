package de.marmaro.krt.ffupdater.installer

import android.content.Context
import com.topjohnwu.superuser.Shell
import de.marmaro.krt.ffupdater.app.MaintainedApp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

/**
 * Copied from https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/RootInstaller.kt
 */
class RootInstaller(
    app: MaintainedApp,
    private val file: File
) :
    SecureAppInstaller(app, file), ForegroundAppInstaller, BackgroundAppInstaller {
    private val installationStatus = CompletableDeferred<InstallResult>()
    private val allowListAbsoluteFilePath = ArrayList<String>()
    private val allowListFileName = ArrayList<String>()

    init {
        MaintainedApp.values().forEach {
            val packageName = it.detail.packageName
            val basePath = "/storage/emulated/0/Android/data/de.marmaro.krt.ffupdater/files/Download"
            allowListAbsoluteFilePath.add("$basePath/${packageName}.apk")
            allowListFileName.add("${packageName}.apk")
        }
    }

    override suspend fun executeInstallerSpecificLogic(context: Context): InstallResult {
        withContext(Dispatchers.IO) {
            install()
        }
        return installationStatus.await()
    }

    private fun install() {
        if (!hasRootPermission()) {
            return
        }
        val size = file.length().toInt()
        val sessionId = createInstallationSession(size) ?: return

        val filePath = file.absolutePath
        val fileName = file.name
        if (!areFileNamesValid(filePath, fileName)) {
            return
        }

        installApp(sessionId, size, filePath, fileName)
    }

    private fun hasRootPermission(): Boolean {
        if (Shell.getShell().isRoot) {
            return true
        }
        installationStatus.complete(InstallResult(false, -90, "Missing root access."))
        return false
    }

    private fun createInstallationSession(size: Int): Int? {
        val response = Shell.cmd("pm install-create -i com.android.vending --user 0 -r -S $size")
            .exec()
            .out

        val sessionIdPattern = Pattern.compile("(\\d+)")
        val sessionIdMatcher = sessionIdPattern.matcher(response[0])
        val found = sessionIdMatcher.find()
        if (!found) {
            installationStatus.complete(InstallResult(false, -91, "Could not find session ID."))
            return null
        }
        return sessionIdMatcher.group(1)?.toInt()
    }

    private fun installApp(sessionId: Int, size: Int, filePath: String, fileName: String) {
        Shell.cmd("cat \"$filePath\" | pm install-write -S $size $sessionId \"${fileName}\"")
            .exec()
        val shellResult = Shell.cmd("pm install-commit $sessionId")
            .exec()

        val result = ArrayList<String>()
        result.addAll(shellResult.out)
        result.addAll(shellResult.err)
        val output = result.joinToString(";")
        installationStatus.complete(InstallResult(shellResult.isSuccess, null, output))
    }

    private fun areFileNamesValid(filePath: String, fileName: String): Boolean {
        if (hasDangerousCharacter(filePath) ||
            hasDangerousCharacter(fileName) ||
            filePath !in allowListAbsoluteFilePath ||
            fileName !in allowListFileName
        ) {
            installationStatus.complete(InstallResult(false, -110, "file path or file name is invalid"))
            return false
        }
        return true
    }

    private fun hasDangerousCharacter(value: String): Boolean {
        val dangerous = listOf(
            "`", ";", "(", ")", "$", "\"", " ", "&", "|", "<", ">", "*", "?", "{", "}", "[", "]", "!", "#"
        )
        return dangerous.any { it in value }
    }
}
