package de.marmaro.krt.ffupdater.utils;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.*;

public class CompareHelperTest {

    @Test
    public void isLessThan() {
        assertTrue(new CompareHelper<>(Duration.ofSeconds(10)).isLessThan(Duration.ofSeconds(20)));
        assertFalse(new CompareHelper<>(Duration.ofSeconds(10)).isLessThan(Duration.ofSeconds(10)));
        assertFalse(new CompareHelper<>(Duration.ofSeconds(10)).isLessThan(Duration.ofSeconds(5)));
    }

    @Test
    public void isLessOrEqualTo() {
        assertTrue(new CompareHelper<>(Duration.ofSeconds(10)).isLessOrEqualTo(Duration.ofSeconds(20)));
        assertTrue(new CompareHelper<>(Duration.ofSeconds(10)).isLessOrEqualTo(Duration.ofSeconds(10)));
        assertFalse(new CompareHelper<>(Duration.ofSeconds(10)).isLessOrEqualTo(Duration.ofSeconds(5)));
    }

    @Test
    public void isGreaterOrEqualTo() {
        assertFalse(new CompareHelper<>(Duration.ofSeconds(10)).isGreaterOrEqualTo(Duration.ofSeconds(20)));
        assertTrue(new CompareHelper<>(Duration.ofSeconds(10)).isGreaterOrEqualTo(Duration.ofSeconds(10)));
        assertTrue(new CompareHelper<>(Duration.ofSeconds(10)).isGreaterOrEqualTo(Duration.ofSeconds(5)));
    }

    @Test
    public void isGreaterThan() {
        assertFalse(new CompareHelper<>(Duration.ofSeconds(10)).isGreaterThan(Duration.ofSeconds(20)));
        assertFalse(new CompareHelper<>(Duration.ofSeconds(10)).isGreaterThan(Duration.ofSeconds(10)));
        assertTrue(new CompareHelper<>(Duration.ofSeconds(10)).isGreaterThan(Duration.ofSeconds(5)));
    }
}