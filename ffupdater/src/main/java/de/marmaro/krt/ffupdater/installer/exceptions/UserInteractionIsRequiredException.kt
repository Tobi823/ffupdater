package de.marmaro.krt.ffupdater.installer.exceptions

import android.content.Context
import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.R

@Keep
class UserInteractionIsRequiredException(errorCode: Int, context: Context) :
    InstallationFailedException(
        "Installation failed because user interaction is required.",
        errorCode,
        context.getString(R.string.session_installer__require_user_interaction)
    )