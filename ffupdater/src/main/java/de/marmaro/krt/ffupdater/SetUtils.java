package de.marmaro.krt.ffupdater;

import java.util.HashSet;
import java.util.Set;

public class SetUtils {

    public static <T> Set<T> createSet(T element) {
        Set<T> set = new HashSet<>();
        set.add(element);
        return set;
    }
}
