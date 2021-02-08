package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.device.ABI

interface Compatibility {
    val minApiLevel: Int
    val supportedAbi: List<ABI> //TODO rename to supportedAbis
}