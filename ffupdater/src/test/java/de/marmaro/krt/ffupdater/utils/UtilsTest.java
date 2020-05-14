package de.marmaro.krt.ffupdater.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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

    @Test
    public void stringToInt_withNull_returnFallback() {
        assertEquals(10, Utils.stringToInt(null, 10));
    }

    @Test
    public void stringToInt_withEmptyString_returnFallback() {
        assertEquals(10, Utils.stringToInt("", 10));
    }

    @Test
    public void stringToInt_withInvalidString_returnFallback() {
        assertEquals(10, Utils.stringToInt("null", 10));
    }

    @Test
    public void stringToInt_withNumber_returnNumber() {
        assertEquals(3, Utils.stringToInt("3", 10));
    }

    @Test(expected = RuntimeException.class)
    public void stringToInt2_withNull_throwRuntimeException() {
        Utils.stringToInt(null);
    }

    @Test(expected = RuntimeException.class)
    public void stringToInt2_withEmpty_throwRuntimeException() {
        Utils.stringToInt("");
    }

    @Test(expected = RuntimeException.class)
    public void stringToInt2_withInvalidNumber_throwRuntimeException() {
        Utils.stringToInt("null");
    }

    @Test
    public void stringToInt2_withNumber_returnNumber() {
        assertEquals(3, Utils.stringToInt("3"));
    }

    @Test
    public void stringsToCharSequenceArray_emptyCollection_returnEmptyArray() {
        assertEquals(0, Utils.stringsToCharSequenceArray(new ArrayList<String>()).length);
    }

    @Test
    public void stringsToCharSequenceArray_withCollection_returnArray() {
        assertArrayEquals(new CharSequence[]{"hi"}, Utils.stringsToCharSequenceArray(Arrays.asList("hi")));
    }

    @Test
    public void integersToCharSequenceArray_emptyCollection_returnEmptyArray() {
        assertEquals(0, Utils.integersToCharSequenceArray(new ArrayList<Integer>()).length);
    }

    @Test
    public void integersToCharSequenceArray_withCollection_returnArray() {
        assertArrayEquals(new CharSequence[]{"42"}, Utils.integersToCharSequenceArray(Arrays.asList(42)));
    }
}