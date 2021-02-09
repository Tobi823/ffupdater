package de.marmaro.krt.ffupdater.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Tobiwan on 03.05.2020.
 */
public class UtilsTest {
    @Test
    public void getVersionAndCodenameFromApiLevel_with1_return10() {
        assertEquals("1.0", Utils.getVersionAndCodenameForApiLevel(1));
    }

    @Test
    public void getVersionAndCodenameFromApiLevel_with29_returnAndroid10() {
        assertEquals("10", Utils.getVersionAndCodenameForApiLevel(29));
    }

    @Test(expected = ParamRuntimeException.class)
    public void getVersionAndCodenameFromApiLevel_with0_returnFallback() {
        Utils.getVersionAndCodenameForApiLevel(0);
    }

    @Test(expected = ParamRuntimeException.class)
    public void getVersionAndCodenameFromApiLevel_with31_returnFallback() {
        Utils.getVersionAndCodenameForApiLevel(31);
    }
}