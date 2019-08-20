package de.marmaro.krt.ffupdater;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Created by Tobiwan on 05.02.2018.
 */
public class VersionTest {
	@Test
	public void compareTo_withSameVersionValue_returnZero() {
		Version local = new Version("58.0.1", browser);
		Version release = new Version("58.0.1", browser);

		assertEquals(0, local.compareTo(release));
	}

	@Test
	public void compareTo_localIsNewerThanCurrentRelease_returnOne() {
		Version local = new Version("58.0.1", browser);
		Version release = new Version("57.0.4", browser);

		assertEquals(1, local.compareTo(release));
	}

	@Test
	public void compareTo_localIsOlderThanCurrentRelease_returnMinusOne() {
		Version local = new Version("57.0.4", browser);
		Version release = new Version("58.0.1", browser);

		assertEquals(-1, local.compareTo(release));
	}

	@Test
	public void equals_withSameVersion_returnTrue() {
		String localVersionName = "58.0.1";
		// prevent compile optimization. localVersionName must be != releaseVersionName
		String releaseVersionName = " 58.0.1 ".trim();

		// verify that the compiler dont optimize the strings by using one single object
		assertTrue(localVersionName != releaseVersionName);
		assertEquals(localVersionName, releaseVersionName);

		Version local = new Version(localVersionName, browser);
		Version release = new Version(releaseVersionName, browser);
		assertEquals(local, release);
	}

	@Test
	public void equals_withDifferentVersion_returnFalse() {
		String localVersionName = "58.0.1";
		String releaseVersionName = "57.0.4";

		Version local = new Version(localVersionName, browser);
		Version release = new Version(releaseVersionName, browser);

		assertEquals(false, local.equals(release));
	}
}