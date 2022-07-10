package de.marmaro.krt.ffupdater.app.eol

import de.marmaro.krt.ffupdater.R

/**
 * https://github.com/fork-maintainers/iceraven-browser
 * https://api.github.com/repos/fork-maintainers/iceraven-browser/releases
 */
class Iceraven : AppBase {
    init {
        TODO("Iceraven is not longer EOL. This class should not be called")
    }

    override val packageName = "io.github.forkmaintainers.iceraven"
    override val displayTitle = R.string.iceraven__title
    override val displayIcon = R.mipmap.ic_logo_iceraven
    override val eolReason = R.string.generic_eol_reason__browser_to_old
}