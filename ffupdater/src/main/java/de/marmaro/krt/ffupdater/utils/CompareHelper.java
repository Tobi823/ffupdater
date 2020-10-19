package de.marmaro.krt.ffupdater.utils;

/**
 * https://stackoverflow.com/a/40217762
 * @param <T>
 */
public class CompareHelper<T> {

    private final Comparable<T> comparable;

    public CompareHelper(Comparable<T> comparable) {
        this.comparable = comparable;
    }

    public boolean isLessThan(T other) {
        return comparable.compareTo(other) < 0;
    }

    public boolean isLessOrEqualTo(T other) {
        return comparable.compareTo(other) <= 0;
    }

    public boolean isGreaterOrEqualTo(T other) {
        return comparable.compareTo(other) >= 0;
    }

    public boolean isGreaterThan(T other) {
        return comparable.compareTo(other) > 0;
    }
}
