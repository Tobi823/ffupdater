package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.device.ABI

interface Compatibility {
    val minApiLevel: Int
    val supportedAbis: List<ABI>
}