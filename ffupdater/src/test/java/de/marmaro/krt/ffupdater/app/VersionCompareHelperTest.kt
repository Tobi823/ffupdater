package de.marmaro.krt.ffupdater.app

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class VersionCompareHelperTest {

    @Test
    fun isAvailableVersionHigher_invalidInstalledVersion_returnTrue() {
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("v1.0.0", "1.0.0"))
    }

    @Test
    fun isAvailableVersionHigher_invalidAvailableVersion_returnTrue() {
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("1.0.0", "v1.0.0"))
    }

    @Test
    fun isAvailableVersionHigher_installedVersionIsNewer_returnTrue() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("1.0.1", "1.0.0"))
    }

    @Test
    fun isAvailableVersionHigher_availableVersionIsNewer_returnTrue() {
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("1.0.0", "1.0.1"))
    }

    @Test
    fun isAvailableVersionHigher_installedVersionIsNewer_brave_returnTrue() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("1.20.103", "1.18.12"))
    }

    @Test
    fun isAvailableVersionHigher_sameVersion_brave_returnFalse() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("1.18.12", "1.18.12"))
    }

    @Test
    fun isAvailableVersionHigher_availableVersionIsNewer_brave_returnTrue() {
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("1.18.12", "1.20.103"))
    }


    @Test
    fun isAvailableVersionHigher_installedVersionIsNewer_bromite_returnFalse() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("90.0.4430.59", "90.0.4430.58"))
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("90.0.4430.59", "89.0.4389.117"))
    }

    @Test
    fun isAvailableVersionHigher_availableVersionIsNewer_bromite_returnTrue() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("89.0.4389.117", "89.0.4389.117"))
    }

    @Test
    fun isAvailableVersionHigher_sameVersion_bromite_returnFalse() {
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("89.0.4389.117", "89.0.4389.118"))
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("89.0.4389.117", "90.0.4430.59"))
    }

    @Test
    fun isAvailableVersionHigher_installedVersionIsNewer_firefoxBeta_returnFalse() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("91.0.0-beta.3", "86.0.0-beta.3"))
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("91.0.0-beta.3", "91.0.0-beta.2"))
    }

    @Test
    fun isAvailableVersionHigher_sameVersion_firefoxBeta_returnFalse() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("91.0.0-beta.3", "91.0.0-beta.3"))
    }

    @Test
    fun isAvailableVersionHigher_availableVersionIsNewer_firefoxBeta_returnTrue() {
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("86.0.0-beta.3", "86.0.0-beta.4"))
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("86.0.0-beta.3", "91.0.0-beta.3"))
    }


    @Test
    fun isAvailableVersionHigher_availableVersionIsNewer_firefoxFocus_returnTrue() {
        assertTrue(VersionCompareHelper.isAvailableVersionHigher("92.1.0", "92.1.1"))
    }

    @Test
    fun isAvailableVersionHigher_sameVersion_firefoxFocus_returnFalse() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("92.1.0", "92.1.0"))
    }

    @Test
    fun isAvailableVersionHigher_installedVersionIsNewer_firefoxFocus_returnFalse() {
        assertFalse(VersionCompareHelper.isAvailableVersionHigher("92.1.1", "92.1.0"))
    }

    @Test
    fun isAvailableVersionEqual_sameVersion_brave_returnFalse() {
        assertTrue(VersionCompareHelper.isAvailableVersionEqual("1.18.12", "1.18.12"))
    }

    @Test
    fun isAvailableVersionEqual_differentVersion_brave_returnTrue() {
        assertFalse(VersionCompareHelper.isAvailableVersionEqual("1.18.12", "1.20.103"))
    }

    @Test
    fun isAvailableVersionEqual_sameVersion_bromite_returnTrue() {
        assertTrue(VersionCompareHelper.isAvailableVersionEqual("89.0.4389.117", "89.0.4389.117"))
    }

    @Test
    fun isAvailableVersionEqual_differentVersion_bromite_returnFalse() {
        assertFalse(VersionCompareHelper.isAvailableVersionEqual("89.0.4389.117", "89.0.4389.118"))
        assertFalse(VersionCompareHelper.isAvailableVersionEqual("89.0.4389.117", "90.0.4430.59"))
    }

    @Test
    fun isAvailableVersionEqual_sameVersion_firefoxBeta_returnFalse() {
        assertTrue(VersionCompareHelper.isAvailableVersionEqual("91.0.0-beta.3", "91.0.0-beta.3"))
    }

    @Test
    fun isAvailableVersionEqual_differentVersion_firefoxBeta_returnTrue() {
        assertFalse(VersionCompareHelper.isAvailableVersionEqual("86.0.0-beta.3", "86.0.0-beta.4"))
        assertFalse(VersionCompareHelper.isAvailableVersionEqual("86.0.0b3", "86.0.0b4"))
        assertFalse(VersionCompareHelper.isAvailableVersionEqual("86.0.0-beta.3", "91.0.0-beta.3"))
    }

    @Test
    fun isAvailableVersionEqual_sameVersion_firefoxFocus_returnFalse() {
        assertTrue(VersionCompareHelper.isAvailableVersionEqual("92.1.0", "92.1.0"))
    }

    @Test
    fun isAvailableVersionEqual_differentVersion_firefoxFocus_returnFalse() {
        assertFalse(VersionCompareHelper.isAvailableVersionEqual("92.1.1", "92.1.0"))
    }
}