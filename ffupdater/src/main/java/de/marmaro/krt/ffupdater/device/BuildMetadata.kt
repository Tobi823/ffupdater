package de.marmaro.krt.ffupdater.device

import de.marmaro.krt.ffupdater.BuildConfig

object BuildMetadata {
    fun isDebugBuild(): Boolean {
        return BuildConfig.BUILD_TYPE == "debug"
    }
}