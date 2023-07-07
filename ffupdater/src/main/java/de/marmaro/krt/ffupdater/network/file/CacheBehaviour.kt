package de.marmaro.krt.ffupdater.network.file

import androidx.annotation.Keep

@Keep
enum class CacheBehaviour { FORCE_NETWORK, USE_CACHE, USE_EVEN_OUTDATED_CACHE }