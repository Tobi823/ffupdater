package de.marmaro.krt.ffupdater.utils

// https://stackoverflow.com/a/51607022
inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T = apply {
    if (condition) block(this)
}