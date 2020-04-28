package de.marmaro.krt.ffupdater.download;

import android.net.Uri;

import org.junit.Test;

import java.net.MalformedURLException;

/**
 * Created by Tobiwan on 13.04.2020.
 */
public class Test1 {

    @Test
    public void test1() throws InterruptedException, MalformedURLException {
        Uri uri = Uri.parse("ftp://ftp.is.co.za/rfc/rfc1808.txt");
        System.out.println(uri);
    }
}
