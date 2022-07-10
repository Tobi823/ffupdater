package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.eol.AppBase
import de.marmaro.krt.ffupdater.app.eol.Lockwise
import de.marmaro.krt.ffupdater.app.eol.UngoogledChromium

// enums with all outdated / end-of-live apps
enum class EolApp(val impl: AppBase) {
    LOCKWISE(Lockwise()),
    UNGOOGLED_CHROMIUM(UngoogledChromium()),
    ;
}
