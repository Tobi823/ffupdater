package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.impl.*
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor

private val apiConsumer = ApiConsumer()
private val deviceAbis = DeviceAbiExtractor.findSupportedAbis()

enum class App(appDetail: BaseApp) {
    FIREFOX_RELEASE(FirefoxRelease(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FIREFOX_BETA(FirefoxBeta(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FIREFOX_NIGHTLY(FirefoxNightly(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FIREFOX_FOCUS(FirefoxFocus(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FIREFOX_KLAR(FirefoxKlar(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    LOCKWISE(Lockwise(apiConsumer = apiConsumer)),
    BRAVE(Brave(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    ICERAVEN(Iceraven(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    BROMITE(Bromite(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    VIVALDI(Vivaldi(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    UNGOOGLED_CHROMIUM(UngoogledChromium(apiConsumer = apiConsumer, deviceAbis = deviceAbis));

    val detail: BaseApp = appDetail
}