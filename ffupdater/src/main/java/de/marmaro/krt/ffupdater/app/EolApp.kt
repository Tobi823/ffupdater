package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.eol.AppBase
import de.marmaro.krt.ffupdater.app.eol.Lockwise

// enums with all outdated / end-of-live apps
enum class EolApp(val impl: AppBase) {
    LOCKWISE(Lockwise()),
    ;
}
