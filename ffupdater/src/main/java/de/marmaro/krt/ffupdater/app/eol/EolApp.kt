package de.marmaro.krt.ffupdater.app.eol

// enums with all outdated / end-of-live apps
enum class EolApp(appDetail: EolBaseApp) {
    LOCKWISE(Lockwise()),
    ;

    val detail: EolBaseApp = appDetail
}