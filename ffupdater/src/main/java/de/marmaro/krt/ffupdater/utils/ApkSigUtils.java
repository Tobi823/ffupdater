package de.marmaro.krt.ffupdater.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.Charset;

/**
 * Created by Tobiwan on 15.05.2020.
 */
public class ApkSigUtils {

    public static class StandardCharsets {
        @SuppressWarnings("CharsetObjectCanBeUsed")
        public static Charset UTF_8 = Charset.forName("UTF-8");
    }

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

    public static long longValueExact(BigInteger value) {
        return value.longValue();
    }

    public static int intValueExact(BigInteger value) {
        return value.intValue();
    }
}
