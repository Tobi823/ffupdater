package de.marmaro.krt.ffupdater.version;

import org.junit.BeforeClass;
import org.junit.Test;

import de.marmaro.krt.ffupdater.App;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class LockwiseIT {
    private static Lockwise lockwise;

    @BeforeClass
    public static void onlyOnce() {
        lockwise = Lockwise.findLatest();
    }

    @Test
    public void getVersion_withNoParams_returnNonEmptyString() {
        assertThat(lockwise.getVersion(), is(not(emptyString())));
        System.out.println(App.LOCKWISE + " - version: " + lockwise.getVersion());
    }

    @Test
    public void getDownloadUrl_withNoParams_returnNonEmptyString() {
        assertThat(lockwise.getVersion(), is(not(emptyString())));
        System.out.println(App.LOCKWISE + " - " + lockwise.getDownloadUrl());
    }
}