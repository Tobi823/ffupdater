package de.marmaro.krt.ffupdater.utils;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
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
        assertEquals("10", Utils.getVersionAndCodenameFromApiLevel(29));
    }

    @Test(expected = ParamRuntimeException.class)
    public void getVersionAndCodenameFromApiLevel_with0_returnFallback() {
        Utils.getVersionAndCodenameFromApiLevel(0);
    }

    @Test(expected = ParamRuntimeException.class)
    public void getVersionAndCodenameFromApiLevel_with31_returnFallback() {
        Utils.getVersionAndCodenameFromApiLevel(31);
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
        assertEquals(0, Utils.stringsToCharSequenceArray(new ArrayList<>()).length);
    }

    @Test
    public void stringsToCharSequenceArray_withCollection_returnArray() {
        assertArrayEquals(new CharSequence[]{"hi"}, Utils.stringsToCharSequenceArray(Collections.singletonList("hi")));
    }

    @Test
    public void integersToCharSequenceArray_emptyCollection_returnEmptyArray() {
        assertEquals(0, Utils.integersToCharSequenceArray(new ArrayList<>()).length);
    }

    @Test
    public void integersToCharSequenceArray_withCollection_returnArray() {
        assertArrayEquals(new CharSequence[]{"42"}, Utils.integersToCharSequenceArray(Collections.singletonList(42)));
    }

    @Test
    public void createSet() {
        final String element = "TEST";
        final Set<String> actual = Utils.createSet(element);
        assertThat(actual, containsInAnyOrder(element));
    }

    @Test
    public void createMap_oneEntry() {
        final String key = "KEY";
        final String value = "VALUE";
        final Map<String, String> actual = Utils.createMap(key, value);
        assertEquals(value, actual.get(key));
    }

    @Test
    public void createMap_twoEntries() {
        final Map<String, String> actual = Utils.createMap("k1", "v1", "k2", "v2");
        assertEquals("v1", actual.get("k1"));
        assertEquals("v2", actual.get("k2"));
    }

    @Test
    public void createUrl() throws MalformedURLException {
        final String urlString = "https://www.mozilla.org/de/firefox/new/";
        final URL url = Utils.createURL(urlString);
        assertEquals(new URL(urlString), url);
    }
}