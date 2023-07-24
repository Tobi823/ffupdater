package de.marmaro.krt.ffupdater.installer.impl

import android.content.Context
import android.os.Environment
import androidx.annotation.Keep
import com.topjohnwu.superuser.Shell
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copied from https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/RootInstaller.kt
 */
@Keep
class RootInstaller(app: App) : AbstractAppInstaller(app) {
    override val type = Installer.ROOT_INSTALLER

    override suspend fun installApkFile(context: Context, file: File) {
        restartInternalShellToGetAlwaysRootPermission()
        fileIsSafeOrThrow(context, file)
        failIfRootPermissionIsMissing()
        val size = file.length().toInt()
        val sessionId = createInstallationSession(size)
        installApp(sessionId, size, file.canonicalPath, file.name)
    }

    private fun fileIsSafeOrThrow(context: Context, file: File) {
        require(!hasDangerousCharacter(file.canonicalPath)) { "File path has dangerous characters: ${file.canonicalPath}" }
        require(!hasDangerousCharacter(file.name)) { "File name has dangerous characters: ${file.name}" }

        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        require(file.parentFile == downloadFolder) { "Wrong folder: ${file.parentFile}" }

        val invalidChars = """\W""".toRegex()
        val appName = app.findImpl().packageName.replace(invalidChars, "_")
        require(file.name.startsWith(appName)) { "Invalid file prefix: ${file.name}" }
        require(file.extension == "apk") { "Invalid file suffix: ${file.name}" }
        require(!file.nameWithoutExtension.contains(invalidChars)) { "Invalid chars in file name: ${file.name}" }
    }

    private fun restartInternalShellToGetAlwaysRootPermission() {
        Shell.getShell().waitAndClose()
        Shell.getShell().waitAndClose()
    }

    private fun failIfRootPermissionIsMissing() {
        val rootGranted = Shell.isAppGrantedRoot()
        if (rootGranted != true) {
            throw InstallationFailedException("Missing root permission. Permission is $rootGranted", -302)
        }
    }

    private suspend fun createInstallationSession(size: Int): Int {
        val response = execute("pm install-create -i com.android.vending --user 0 -r -S $size")
        val result = response[0]

        val sessionIdMatch = Regex("""\d+""").find(result)
        checkNotNull(sessionIdMatch) { "Can't find session id with regex pattern. Output: $result" }

        val sessionId = sessionIdMatch.groups[0]
        checkNotNull(sessionId) { "Can't find match group containing the session id. Output: $result" }

        return sessionId.value.toInt()
    }

    private suspend fun installApp(sessionId: Int, size: Int, filePath: String, fileName: String) {
        execute("cat \"$filePath\" | pm install-write -S $size $sessionId \"${fileName}\"")
        execute("pm install-commit $sessionId")
    }

    private suspend fun execute(command: String): List<String> {
        return withContext(Dispatchers.IO) {
            val result = Shell.cmd(command).exec()
            if (result.code != 0) {
                throw InstallationFailedException(
                    "Root command '$command' failed. Result code is: '${result.code}', " +
                            "stdout: '${result.out}', stderr: '${result.err}'",
                    -403
                )
            }
            result.out
        }
    }

    private fun hasDangerousCharacter(value: String): Boolean {
        val dangerous = listOf(
            "`", ";", "(", ")", "$", "\"", " ", "&", "|", "<", ">", "*", "?", "{", "}", "[", "]", "!", "#"
        )
        return dangerous.any { it in value }
    }
}
