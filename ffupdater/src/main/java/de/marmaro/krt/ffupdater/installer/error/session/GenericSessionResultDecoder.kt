package de.marmaro.krt.ffupdater.installer.error.session

import android.content.Context
import android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE
import android.content.pm.PackageInstaller.STATUS_FAILURE
import android.content.pm.PackageInstaller.STATUS_FAILURE_ABORTED
import android.content.pm.PackageInstaller.STATUS_FAILURE_BLOCKED
import android.content.pm.PackageInstaller.STATUS_FAILURE_CONFLICT
import android.content.pm.PackageInstaller.STATUS_FAILURE_INCOMPATIBLE
import android.content.pm.PackageInstaller.STATUS_FAILURE_INVALID
import android.content.pm.PackageInstaller.STATUS_FAILURE_STORAGE
import android.os.Bundle
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.R

@Keep
object GenericSessionResultDecoder {
    fun getShortErrorMessage(status: Int, bundle: Bundle?): String {
        val errorMessage = when (status) {
            STATUS_FAILURE -> "The installation failed in a generic way."
            STATUS_FAILURE_ABORTED -> "The installation failed because it was actively aborted."
            STATUS_FAILURE_BLOCKED -> "The installation failed because it was blocked."
            STATUS_FAILURE_CONFLICT -> "The installation failed because it conflicts (or is inconsistent with) with another package already installed on the device."
            STATUS_FAILURE_INCOMPATIBLE -> "The installation failed because it is fundamentally incompatible with this device."
            STATUS_FAILURE_INVALID -> "The installation failed because one or more of the APKs was invalid."
            STATUS_FAILURE_STORAGE -> "The installation failed because of storage issues."
            else -> "The installation failed. Status: $status."
        }
        val extraMessage = bundle?.getString(EXTRA_STATUS_MESSAGE) ?: return errorMessage
        return "$extraMessage. $errorMessage"
    }

    fun getTranslatedErrorMessage(context: Context, status: Int, bundle: Bundle?): String {
        val errorMessage = when (status) {
            STATUS_FAILURE -> context.getString(R.string.session_installer__status_failure)
            STATUS_FAILURE_ABORTED -> context.getString(R.string.session_installer__status_failure_aborted)
            STATUS_FAILURE_BLOCKED -> context.getString(R.string.session_installer__status_failure_blocked)
            STATUS_FAILURE_CONFLICT -> context.getString(R.string.session_installer__status_failure_conflict)
            STATUS_FAILURE_INCOMPATIBLE -> context.getString(R.string.session_installer__status_failure_incompatible)
            STATUS_FAILURE_INVALID -> context.getString(R.string.session_installer__status_failure_invalid)
            STATUS_FAILURE_STORAGE -> context.getString(R.string.session_installer__status_failure_storage)
            else -> "The installation failed. Status: $status."
        }
        val extraMessage = bundle?.getString(EXTRA_STATUS_MESSAGE) ?: return errorMessage
        return "$extraMessage. $errorMessage"
    }
}