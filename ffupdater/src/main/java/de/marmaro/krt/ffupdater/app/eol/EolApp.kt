package de.marmaro.krt.ffupdater.app.eol

// enums with all outdated / end-of-live apps
enum class EolApp(val impl: EolAppBase) {
    ICERAVEN(Iceraven()),
    LOCKWISE(Lockwise()),
    UNGOOGLED_CHROMIUM(UngoogledChromium()),
    ;
}
