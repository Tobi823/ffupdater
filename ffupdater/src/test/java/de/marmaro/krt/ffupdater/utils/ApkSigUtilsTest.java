package de.marmaro.krt.ffupdater.utils;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by Tobiwan on 15.05.2020.
 */
public class ApkSigUtilsTest {

    @Annotation1("abc")
    static class Test1 {

        @Annotation1("ghi")
        public String field1;

        @Annotation2("def")
        public static void method1() {

        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Annotation1 {
        String value();
    }

    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Annotation2 {
        String value();
    }

    @Test
    public void getDeclaredAnnotation_forClassTest1AndCorrectAnnotation_returnAnnotation() {
        Annotation1 annotation1 = ApkSigUtils.getDeclaredAnnotation(Test1.class, Annotation1.class);
        assertNotNull(annotation1);
        assertEquals("abc", annotation1.value());
    }

    @Test
    public void getDeclaredAnnotation_forClassTest1AndWrongAnnotation_returnNull() {
        Annotation2 annotation2 = ApkSigUtils.getDeclaredAnnotation(Test1.class, Annotation2.class);
        assertNull(annotation2);
    }

    @Test
    public void getDeclaredAnnotation_forMethod1AndCorrectAnnotation_returnAnnotation() throws NoSuchMethodException {
        Annotation1 annotation1 = ApkSigUtils.getDeclaredAnnotation(Test1.class.getMethod("method1"), Annotation1.class);
        assertNull(annotation1);

    }

    @Test
    public void getDeclaredAnnotation_forMethod1AndWrongAnnotation_returnNull() throws NoSuchMethodException {
        Annotation2 annotation2 = ApkSigUtils.getDeclaredAnnotation(Test1.class.getMethod("method1"), Annotation2.class);
        assertNotNull(annotation2);
        assertEquals("def", annotation2.value());
    }

    @Test
    public void getDeclaredAnnotation_forField1AndCorrectAnnotation_returnAnnotation() throws NoSuchFieldException {
        Annotation1 annotation1 = ApkSigUtils.getDeclaredAnnotation(Test1.class.getField("field1"), Annotation1.class);
        assertNotNull(annotation1);
        assertEquals("ghi", annotation1.value());
    }

    @Test
    public void getDeclaredAnnotation_forField1AndWrongAnnotation_returnNull() throws NoSuchFieldException {
        Annotation2 annotation2 = ApkSigUtils.getDeclaredAnnotation(Test1.class.getField("field1"), Annotation2.class);
        assertNull(annotation2);
    }
}