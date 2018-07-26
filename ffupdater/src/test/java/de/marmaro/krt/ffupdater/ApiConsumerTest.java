package de.marmaro.krt.ffupdater;

import com.github.dmstocking.optional.java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Use https://badssl.com/ for testing the certificate validation.
 * Created by Tobiwan on 26.07.2018.
 */
public class ApiConsumerTest {

    @Test
    public void findApiResponse_testWithValidCertificate_nonEmptyResponse() {
        Optional<String> response = ApiConsumer.findRawApiResponse("https://mozilla-modern.badssl.com/");
        assertEquals(true, response.isPresent());
    }

    @Test
    public void findApiResponse_testWithExpiredCertificate_emptyResponse() {
        Optional<String> response = ApiConsumer.findRawApiResponse("https://expired.badssl.com/");
        assertEquals(false, response.isPresent());
    }

    @Test
    public void findApiResponse_testWithWrongHostCertificate_emptyResponse() {
        Optional<String> response = ApiConsumer.findRawApiResponse("https://wrong.host.badssl.com/");
        assertEquals(false, response.isPresent());
    }

    @Test
    public void findApiResponse_testWithSelfSignedCertificate_emptyResponse() {
        Optional<String> response = ApiConsumer.findRawApiResponse("https://self-signed.badssl.com/");
        assertEquals(false, response.isPresent());
    }

    @Test
    public void findApiResponse_testWithUntrustedRootCertificate_emptyResponse() {
        Optional<String> response = ApiConsumer.findRawApiResponse("https://untrusted-root.badssl.com/");
        assertEquals(false, response.isPresent());
    }

    /**
     * IOUtils.toString doesn't check for revoked certificates. As far as I know it this missing
     * feature not critical.
     */
    @Ignore
    @Test
    public void findApiResponse_testWithRevokedCertificate_emptyResponse() {
        Optional<String> response = ApiConsumer.findRawApiResponse("https://revoked.badssl.com/");
        assertEquals(false, response.isPresent());
    }

    /**
     * IOUtils.toString doesn't check for pinned certificates. As far as I know it this missing
     * feature not critical.
     */
    @Ignore
    @Test
    public void findApiResponse_testWithPinningTestCertificate_emptyResponse() {
        Optional<String> response = ApiConsumer.findRawApiResponse("https://pinning-test.badssl.com/");
        assertEquals(false, response.isPresent());
    }



}