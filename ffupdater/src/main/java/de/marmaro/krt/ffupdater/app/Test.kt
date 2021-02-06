package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.device.ABI
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class Test(
        private var app: App
) {
    private var cache: Deferred<UpdateCheckResult>? = null
//    fun test(): Deferred<UpdateCheckResult> {
//        if (cache != null && cache!!.isActive) {
//            return cache!!
//        }
//        cache = GlobalScope.async {
//            app.updateCheckBlocking(null, ABI.AARCH64)
//        }
//        return cache!!
//        // Wrapper, damit immer nur ein Deferred erstellt wird
//    }
}