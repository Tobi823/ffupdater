package de.marmaro.krt.ffupdater.app

import kotlinx.coroutines.Deferred

class Test {
    fun main(args: Array<String>) {
        println("Hello, world!")
    }
//    fun test() {
//        val test = UpdateCheckResult(
//                isUpdateAvailable = false,
//                downloadUrl = URL("https://"),
//                version = "hi",
//                metadata = Collections.emptyMap()
//                )
//        test.version = "a"
//    }
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