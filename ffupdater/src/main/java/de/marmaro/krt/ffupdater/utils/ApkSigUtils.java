package de.marmaro.krt.ffupdater.utils;

import android.os.Build;

import androidx.annotation.Nullable;

import com.android.apksig.internal.apk.ApkSigningBlockUtils.SupportedSignature;
import com.android.apksig.internal.apk.ContentDigestAlgorithm;
import com.android.apksig.internal.apk.v1.V1SchemeVerifier;
import com.android.apksig.util.DataSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extract most of the changes of the ApkSign sources to this class.
 * (To simplify an upgrade of the ApkSign sources in the future)
 */
public class ApkSigUtils {

    public static <T> T getDeclaredAnnotation(AnnotatedElement annotatedElement, Class<T> annotationClazz) {
        for (Annotation element : annotatedElement.getDeclaredAnnotations()) {
            if (element.annotationType() == annotationClazz) {
                return (T) element;
            }
        }
        return null;
    }

    public static String getTypeName(Type type) {
        return type.toString();
    }

    public static class StandardCharsets {
        @SuppressWarnings("CharsetObjectCanBeUsed")
        public static Charset UTF_8 = Charset.forName("UTF-8");
    }

    public static class BigIntegerUtils {
        public static long longValueExact(BigInteger value) {
            if (/*mag.length <= 2 &&*/ value.bitLength() <= 63) {
                return value.longValue();
            } else {
                throw new ArithmeticException("BigInteger out of long range");
            }
        }
        public static int intValueExact(BigInteger value) {
            if (/*value.mag.length <= 1 &&*/ value.bitLength() <= 31) {
                return value.intValue();
            } else {
                throw new ArithmeticException("BigInteger out of int range");
            }
        }
    }

    public static class Math {
        public static int toIntExact(long value) {
//            Math.toIntExact(sizeToCopy)
            if ((int) value != value) {
                throw new ArithmeticException("integer overflow");
            }
            return (int) value;
        }
    }

    public static class X509CertificateUtils {
        public static void verify(X509Certificate x509Certificate, PublicKey key, Provider sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                x509Certificate.verify(key, sigProvider);
            } else {
                throw new RuntimeException("not yet implemented");
            }
        }
    }

    public static class ApkSigningBlockUtilsUtils {
        public static Set<ContentDigestAlgorithm> filterSha256OrSha512Algorithm(Set<ContentDigestAlgorithm> digestAlgorithms) {
//            Map<ContentDigestAlgorithm, byte[]> contentDigests = new HashMap<>();
//            Set<ContentDigestAlgorithm> oneMbChunkBasedAlgorithm = digestAlgorithms.stream()
//                    .filter(a -> a == ContentDigestAlgorithm.CHUNKED_SHA256 ||
//                            a == ContentDigestAlgorithm.CHUNKED_SHA512)
//                    .collect(Collectors.toSet());
            Set<ContentDigestAlgorithm> oneMbChunkBasedAlgorithm = new HashSet<>();
            if (digestAlgorithms.contains(ContentDigestAlgorithm.CHUNKED_SHA256)) {
                oneMbChunkBasedAlgorithm.add(ContentDigestAlgorithm.CHUNKED_SHA256);
            }
            if (digestAlgorithms.contains(ContentDigestAlgorithm.CHUNKED_SHA512)) {
                oneMbChunkBasedAlgorithm.add(ContentDigestAlgorithm.CHUNKED_SHA512);
            }
            return oneMbChunkBasedAlgorithm;
        }

        public static List<SupportedSignature> sortByAlgorithmId(Map<Integer, SupportedSignature> bestSigAlgorithmOnSdkVersion) {
//            return bestSigAlgorithmOnSdkVersion.values().stream()
//                    .sorted((sig1, sig2) -> Integer.compare(
//                            sig1.algorithm.getId(), sig2.algorithm.getId()))
//                    .collect(Collectors.toList());
            List<SupportedSignature> tempList = new ArrayList<>(bestSigAlgorithmOnSdkVersion.values());
            Collections.sort(tempList, (sig1, sig2) -> Integer.compare(
                    sig1.algorithm.getId(), sig2.algorithm.getId()));
            return tempList;
        }
    }

    public static class ChainedDataSourceUtils {
        public static long sumSizes(DataSource... sources) {
//            mTotalSize = Arrays.stream(sources).mapToLong(src -> src.size()).sum();
            long totalSize = 0;
            for (DataSource source : sources) {
                totalSize += source.size();
            }
            return totalSize;
        }
    }

    public static class DefaultApkSignerEngineUtils {
        @Nullable
        public static V1SchemeVerifier.NamedDigest findDigest(Collection<V1SchemeVerifier.NamedDigest> digests, String alg) {
//            Optional<V1SchemeVerifier.NamedDigest> extractedDigest =
//                    V1SchemeVerifier.getDigestsToVerify(
//                            entry.getValue(), "-Digest", mMinSdkVersion, Integer.MAX_VALUE)
//                            .stream()
//                            .filter(d -> d.jcaDigestAlgorithm == alg)
//                            .findFirst();
//
//            extractedDigest.ifPresent(
//                    namedDigest -> mOutputJarEntryDigests.put(entryName, namedDigest.digest));
            for (V1SchemeVerifier.NamedDigest digest : digests) {
                //noinspection StringEquality
                if (digest.jcaDigestAlgorithm == alg) {
                    return digest;
                }
            }
            return null;
        }
    }
}
