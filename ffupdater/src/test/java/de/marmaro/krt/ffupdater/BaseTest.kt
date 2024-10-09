package de.marmaro.krt.ffupdater

import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterAll

abstract class BaseTest {
    companion object {
        @AfterAll
        @JvmStatic
        fun afterAll() {
            unmockkAll()
        }
    }
}