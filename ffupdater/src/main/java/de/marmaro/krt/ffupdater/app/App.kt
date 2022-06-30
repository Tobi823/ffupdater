package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.maintained.*
import de.marmaro.krt.ffupdater.app.network.ApiConsumer
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor

private val apiConsumer = ApiConsumer()
private val deviceAbis = DeviceAbiExtractor.findSupportedAbis()

enum class App(appDetail: BaseApp) {
    BRAVE(Brave(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    BRAVE_BETA(BraveBeta(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    BRAVE_NIGHTLY(BraveNightly(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    BROMITE(Bromite(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    BROMITE_SYSTEMWEBVIEW(BromiteSystemWebView(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FFUPDATER(FFUpdater(apiConsumer = apiConsumer)),
    FIREFOX_BETA(FirefoxBeta(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FIREFOX_FOCUS(FirefoxFocus(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FIREFOX_KLAR(FirefoxKlar(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FIREFOX_NIGHTLY(FirefoxNightly(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    FIREFOX_RELEASE(FirefoxRelease(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    KIWI(Kiwi(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    VIVALDI(Vivaldi(apiConsumer = apiConsumer, deviceAbis = deviceAbis)),
    ;

    val detail: BaseApp = appDetail
}