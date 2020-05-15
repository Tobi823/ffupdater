package de.marmaro.krt.ffupdater.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Created by Tobiwan on 15.05.2020.
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
}
