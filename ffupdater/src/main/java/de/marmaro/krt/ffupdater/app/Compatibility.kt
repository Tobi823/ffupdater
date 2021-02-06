package de.marmaro.krt.ffupdater.app

import de.marmaro.krt.ffupdater.device.ABI

interface Compatibility {
    fun minApiLevel(): Int
    fun supportedAbi(): List<ABI>
}