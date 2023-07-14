package de.marmaro.krt.ffupdater.security

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.Signature
import de.marmaro.krt.ffupdater.BaseTest
import de.marmaro.krt.ffupdater.app.App
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.security.cert.CertificateException
import kotlin.io.path.createTempFile
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class FingerprintValidatorTest : BaseTest() {

    @MockK
    lateinit var packageManager: PackageManager

    @MockK
    lateinit var signature: Signature

    companion object {
        lateinit var signatureBytes: ByteArray

        @Suppress("SpellCheckingInspection")
        const val signatureFingerprint = "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"
        val file = createTempFile("FIREFOX_RELEASE__2021_04_30__244384144", ".apk").toFile()!!

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            val path = "src/test/resources/de/marmaro/krt/ffupdater/security/FirefoxReleaseAppSignature.bin"
            signatureBytes = File(path).readBytes()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            file.delete()
        }
    }

    @Test
    fun checkApkFile_withCorrectSignature_returnValid() {
        val packageInfo = PackageInfo()
        @Suppress("DEPRECATION")
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns signatureBytes
        every {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
        } returns packageInfo

        val actual =
            runBlocking { FingerprintValidator.checkApkFile(packageManager, file, App.FIREFOX_RELEASE.findImpl()) }
        assertTrue(actual.isValid)
        assertEquals(signatureFingerprint, actual.hexString)
        assertEquals(signatureFingerprint, App.FIREFOX_RELEASE.findImpl().signatureHash)
    }

    @Test
    fun checkApkFile_withWrongApp_returnInvalid() {
        val packageInfo = PackageInfo()
        @Suppress("DEPRECATION")
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns signatureBytes
        every {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
        } returns packageInfo

        val actual = runBlocking { FingerprintValidator.checkApkFile(packageManager, file, App.BRAVE.findImpl()) }
        assertFalse(actual.isValid)
    }

    @Test
    fun checkApkFile_withIncorrectSignature_throwException() {
        val packageInfo = PackageInfo()
        @Suppress("DEPRECATION")
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns Random.nextBytes(938)
        every {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
        } returns packageInfo

        assertThrows(CertificateException::class.java) {
            runBlocking {
                FingerprintValidator.checkApkFile(packageManager, file, App.FIREFOX_RELEASE.findImpl())
            }
        }
    }

    @Test
    fun checkInstalledApp_withFirefoxRelease_returnValid() {
        val packageInfo = PackageInfo()
        @Suppress("DEPRECATION")
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns signatureBytes
        every {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(App.FIREFOX_RELEASE.findImpl().packageName, GET_SIGNATURES)
        } returns packageInfo
        val actual = runBlocking {
            FingerprintValidator.checkInstalledApp(packageManager, App.FIREFOX_RELEASE.findImpl())
        }
        assertTrue(actual.isValid)
        assertEquals(signatureFingerprint, actual.hexString)
        assertEquals(signatureFingerprint, App.FIREFOX_RELEASE.findImpl().signatureHash)
    }

    @Test
    fun checkInstalledApp_withInvalidCertificate_throwException() {
        val packageInfo = PackageInfo()
        @Suppress("DEPRECATION")
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns Random.nextBytes(938)
        every {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(App.FIREFOX_RELEASE.findImpl().packageName, GET_SIGNATURES)
        } returns packageInfo

        assertThrows(CertificateException::class.java) {
            runBlocking {
                FingerprintValidator.checkInstalledApp(packageManager, App.FIREFOX_RELEASE.findImpl())
            }
        }
    }
}