package de.marmaro.krt.ffupdater.app.eol

import de.marmaro.krt.ffupdater.R

/**
 * https://github.com/ungoogled-software/ungoogled-chromium-android/releases
 */

class UngoogledChromium : EolAppBase {
    override val packageName = "org.ungoogled.chromium.stable"
    override val displayTitle = R.string.ungoogled_chromium__title
    override val displayIcon = R.mipmap.ic_logo_ungoogled_chromium
    override val eolReason = R.string.generic_eol_reason__browser_to_old
}