package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.app.impl.Brave

class Test {
    fun main(args: Array<String>) {
        println("Hello, world!")
    }

    fun test() {
        val app = Brave()
        val wrapper: App = CacheWrapper(app)
        wrapper.
    }
}
