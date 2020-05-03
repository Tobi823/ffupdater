package de.marmaro.krt.ffupdater.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Tobiwan on 03.05.2020.
 */
public class UtilsTest {

    @Test
    public void convertNullToEmptyString_willNull_returnEmptyString() {
        assertEquals("", Utils.convertNullToEmptyString(null));
    }

    @Test
    public void convertNullToEmptyString_withEmptyString_returnEmptyString() {
        assertEquals("", Utils.convertNullToEmptyString(""));
    }

    @Test
    public void convertNullToEmptyString_withString_returnString() {
        assertEquals("hello world", Utils.convertNullToEmptyString("hello world"));
    }

    @Test
    public void getVersionAndCodenameFromApiLevel_with1_return10() {
        assertEquals("1.0", Utils.getVersionAndCodenameFromApiLevel(1));
    }

    @Test
    public void getVersionAndCodenameFromApiLevel_with29_returnAndroid10() {
        System.out.println(Utils.getVersionAndCodenameFromApiLevel(15));
        assertEquals("10 (Android10)", Utils.getVersionAndCodenameFromApiLevel(29));
    }

    @Test
    public void getVersionAndCodenameFromApiLevel_with0_returnFallback() {
        assertEquals("API Level 0", Utils.getVersionAndCodenameFromApiLevel(0));
    }

    @Test
    public void getVersionAndCodenameFromApiLevel_with30_returnFallback() {
        assertEquals("API Level 30", Utils.getVersionAndCodenameFromApiLevel(30));
    }
}