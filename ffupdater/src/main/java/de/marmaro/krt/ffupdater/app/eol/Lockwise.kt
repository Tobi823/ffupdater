package de.marmaro.krt.ffupdater.app.eol

import de.marmaro.krt.ffupdater.R

/**
 * https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases
 * https://www.apkmirror.com/apk/mozilla/firefox-lockwise/
 */
class Lockwise : AppBase {
    override val packageName = "mozilla.lockbox"
    override val displayTitle = R.string.lockwise__title
    override val displayIcon = R.mipmap.ic_logo_lockwise
    override val eolReason = R.string.lockwise__eol_reason
}