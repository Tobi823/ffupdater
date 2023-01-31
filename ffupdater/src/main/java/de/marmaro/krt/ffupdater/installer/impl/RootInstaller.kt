package de.marmaro.krt.ffupdater.installer.impl

import android.content.Context
import com.topjohnwu.superuser.Shell
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.installer.exception.InstallationFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copied from https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/RootInstaller.kt
 */
class RootInstaller(app: App) : AbstractAppInstaller(app) {
    override val type = Installer.ROOT_INSTALLER

    override suspend fun executeInstallerSpecificLogic(context: Context, file: File) {
        val filePath = file.absolutePath
        val fileName = file.name
        require(!hasDangerousCharacter(filePath))
        require(!hasDangerousCharacter(fileName))
        require(filePath == "/storage/emulated/0/Android/data/de.marmaro.krt.ffupdater/files/Download/${app.impl.packageName}.apk")
        require(fileName == "${app.impl.packageName}.apk")

        failIfRootPermissionIsMissing()
        val size = file.length().toInt()
        val sessionId = createInstallationSession(size)
        installApp(sessionId, size, filePath, fileName)
    }

    private fun failIfRootPermissionIsMissing() {
        if (!Shell.getShell().isRoot) {
            throw InstallationFailedException("Missing root permission", -302)
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
