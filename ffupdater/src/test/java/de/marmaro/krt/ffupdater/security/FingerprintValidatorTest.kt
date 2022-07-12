package de.marmaro.krt.ffupdater.security

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.Signature
import de.marmaro.krt.ffupdater.app.App
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.security.cert.CertificateException
import kotlin.io.path.createTempFile
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class FingerprintValidatorTest {

    @MockK
    lateinit var packageManager: PackageManager

    @MockK
    lateinit var signature: Signature
    private lateinit var fingerprintValidator: FingerprintValidator

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

    @BeforeEach
    fun setUp() {
        fingerprintValidator = FingerprintValidator(packageManager)
    }

    @Test
    fun checkApkFile_withCorrectSignature_returnValid() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns signatureBytes
        every {
            packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
        } returns packageInfo

        val actual =
            runBlocking { fingerprintValidator.checkApkFile(file, App.FIREFOX_RELEASE.impl) }
        assertTrue(actual.isValid)
        assertEquals(signatureFingerprint, actual.hexString)
        assertEquals(signatureFingerprint, App.FIREFOX_RELEASE.impl.signatureHash)
    }

    @Test
    fun checkApkFile_withWrongApp_returnInvalid() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns signatureBytes
        every {
            packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
        } returns packageInfo

        val actual = runBlocking { fingerprintValidator.checkApkFile(file, App.BRAVE.impl) }
        assertFalse(actual.isValid)
    }

    @Test
    fun checkApkFile_withIncorrectSignature_throwException() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns Random.nextBytes(938)
        every {
            packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
        } returns packageInfo

        assertThrows(CertificateException::class.java) {
            runBlocking {
                fingerprintValidator.checkApkFile(file, App.FIREFOX_RELEASE.impl)
            }
        }
    }

    @Test
    fun checkInstalledApp_withFirefoxRelease_returnValid() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns signatureBytes
        every {
            packageManager.getPackageInfo(App.FIREFOX_RELEASE.impl.packageName, GET_SIGNATURES)
        } returns packageInfo
        val actual = runBlocking {
            fingerprintValidator.checkInstalledApp(App.FIREFOX_RELEASE.impl)
        }
        assertTrue(actual.isValid)
        assertEquals(signatureFingerprint, actual.hexString)
        assertEquals(signatureFingerprint, App.FIREFOX_RELEASE.impl.signatureHash)
    }

    @Test
    fun checkInstalledApp_withInvalidCertificate_throwException() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns Random.nextBytes(938)
        every {
            packageManager.getPackageInfo(App.FIREFOX_RELEASE.impl.packageName, GET_SIGNATURES)
        } returns packageInfo

        assertThrows(CertificateException::class.java) {
            runBlocking {
                fingerprintValidator.checkInstalledApp(App.FIREFOX_RELEASE.impl)
            }
        }
    }
}