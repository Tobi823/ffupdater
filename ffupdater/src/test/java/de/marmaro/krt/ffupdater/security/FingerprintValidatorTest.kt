package de.marmaro.krt.ffupdater.security

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.Signature
import de.marmaro.krt.ffupdater.app.App
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import kotlin.random.Random


@RunWith(MockitoJUnitRunner::class)
class FingerprintValidatorTest {

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var signature: Signature
    private lateinit var fingerprintValidator: FingerprintValidator

    companion object {
        lateinit var signatureBytes: ByteArray
        const val signatureFingerprint = "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"

        @BeforeClass
        @JvmStatic
        fun setUpStatic() {
            val path = "src/test/resources/de/marmaro/krt/ffupdater/security/FirefoxReleaseAppSignature.bin"
            signatureBytes = File(path).readBytes()
        }
    }

    @Before
    fun setUp() {
        fingerprintValidator = FingerprintValidator(packageManager)
    }

    @Test
    fun checkApkFile_withCorrectSignature_returnValid() {
        val file = File("file.apk")
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        `when`(signature.toByteArray()).thenReturn(signatureBytes)
        `when`(packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES))
                .thenReturn(packageInfo)

        val actual = runBlocking { fingerprintValidator.checkApkFile(file, App.FIREFOX_RELEASE) }
        assertTrue(actual.isValid)
        assertEquals(signatureFingerprint, actual.hexString)
        assertEquals(signatureFingerprint, App.FIREFOX_RELEASE.detail.signatureHash)
    }

    @Test
    fun checkApkFile_withWrongApp_returnInvalid() {
        val file = File("file.apk")
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        `when`(signature.toByteArray()).thenReturn(signatureBytes)
        `when`(packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES))
                .thenReturn(packageInfo)

        val actual = runBlocking { fingerprintValidator.checkApkFile(file, App.BRAVE) }
        assertFalse(actual.isValid)
    }

    @Test(expected = FingerprintValidator.UnableCheckApkException::class)
    fun checkApkFile_withIncorrectSignature_throwException() {
        val file = File("file.apk")
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        `when`(signature.toByteArray()).thenReturn(Random.nextBytes(938))
        `when`(packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES))
                .thenReturn(packageInfo)

        runBlocking { fingerprintValidator.checkApkFile(file, App.FIREFOX_RELEASE) }
    }

    @Test
    fun checkInstalledApp_withFirefoxRelease_returnValid() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        `when`(signature.toByteArray()).thenReturn(signatureBytes)
        `when`(packageManager.getPackageInfo(App.FIREFOX_RELEASE.detail.packageName, GET_SIGNATURES))
                .thenReturn(packageInfo)
        val actual = fingerprintValidator.checkInstalledApp(App.FIREFOX_RELEASE)
        assertTrue(actual.isValid)
        assertEquals(signatureFingerprint, actual.hexString)
        assertEquals(signatureFingerprint, App.FIREFOX_RELEASE.detail.signatureHash)
    }

    @Test
    fun checkInstalledApp_withAppIsNotInstalled_returnInvalid() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        `when`(packageManager.getPackageInfo(App.LOCKWISE.detail.packageName, GET_SIGNATURES))
                .thenThrow(PackageManager.NameNotFoundException())
        val actual = fingerprintValidator.checkInstalledApp(App.LOCKWISE)
        assertFalse(actual.isValid)
    }

    @Test(expected = FingerprintValidator.UnableCheckApkException::class)
    fun checkInstalledApp_withInvalidCertificate_throwException() {
        val packageInfo = PackageInfo()
        packageInfo.signatures = arrayOf(signature)
        `when`(signature.toByteArray()).thenReturn(Random.nextBytes(938))
        `when`(packageManager.getPackageInfo(App.FIREFOX_RELEASE.detail.packageName, GET_SIGNATURES))
                .thenReturn(packageInfo)
        fingerprintValidator.checkInstalledApp(App.FIREFOX_RELEASE)
    }
}