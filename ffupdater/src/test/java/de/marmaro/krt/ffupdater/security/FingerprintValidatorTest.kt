package de.marmaro.krt.ffupdater.security

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.Signature
import de.marmaro.krt.ffupdater.app.App
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.random.Random


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

        @BeforeClass
        @JvmStatic
        fun setUpStatic() {
            val path = "src/test/resources/de/marmaro/krt/ffupdater/security/FirefoxReleaseAppSignature.bin"
            signatureBytes = File(path).readBytes()
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            file.delete()
        }
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
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

        val actual = runBlocking { fingerprintValidator.checkApkFile(file, App.FIREFOX_RELEASE) }
        assertTrue(actual.isValid)
        assertEquals(signatureFingerprint, actual.hexString)
        assertEquals(signatureFingerprint, App.FIREFOX_RELEASE.detail.signatureHash)
    }

    @Test
    fun checkApkFile_withWrongApp_returnInvalid() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns signatureBytes
        every {
            packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
        } returns packageInfo

        val actual = runBlocking { fingerprintValidator.checkApkFile(file, App.BRAVE) }
        assertFalse(actual.isValid)
    }

    @Test(expected = FingerprintValidator.UnableCheckApkException::class)
    fun checkApkFile_withIncorrectSignature_throwException() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns Random.nextBytes(938)
        every {
            packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
        } returns packageInfo
        runBlocking { fingerprintValidator.checkApkFile(file, App.FIREFOX_RELEASE) }
    }

    @Test
    fun checkInstalledApp_withFirefoxRelease_returnValid() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns signatureBytes
        every {
            packageManager.getPackageInfo(App.FIREFOX_RELEASE.detail.packageName, GET_SIGNATURES)
        } returns packageInfo
        val actual = fingerprintValidator.checkInstalledApp(App.FIREFOX_RELEASE)
        assertTrue(actual.isValid)
        assertEquals(signatureFingerprint, actual.hexString)
        assertEquals(signatureFingerprint, App.FIREFOX_RELEASE.detail.signatureHash)
    }

    @Test
    fun checkInstalledApp_withAppIsNotInstalled_returnInvalid() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every {
            packageManager.getPackageInfo(App.LOCKWISE.detail.packageName, GET_SIGNATURES)
        } throws PackageManager.NameNotFoundException()
        val actual = fingerprintValidator.checkInstalledApp(App.LOCKWISE)
        assertFalse(actual.isValid)
    }

    @Test(expected = FingerprintValidator.UnableCheckApkException::class)
    fun checkInstalledApp_withInvalidCertificate_throwException() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        every { signature.toByteArray() } returns Random.nextBytes(938)
        every {
            packageManager.getPackageInfo(App.FIREFOX_RELEASE.detail.packageName, GET_SIGNATURES)
        } returns packageInfo
        fingerprintValidator.checkInstalledApp(App.FIREFOX_RELEASE)
    }
}