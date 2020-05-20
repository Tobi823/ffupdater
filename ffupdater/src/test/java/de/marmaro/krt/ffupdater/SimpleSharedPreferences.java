package de.marmaro.krt.ffupdater;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Tobiwan on 21.05.2020.
 */
public class SimpleSharedPreferences implements SharedPreferences {

    private Map<String, String> map = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
        throw new RuntimeException("not supported");
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        throw new RuntimeException("not supported");
    }

    @Override
    public int getInt(String key, int defValue) {
        if (map.containsKey(key)) {
            //noinspection ConstantConditions
            return Integer.parseInt(map.get(key));
        }
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        if (map.containsKey(key)) {
            //noinspection ConstantConditions
            return Long.parseLong(map.get(key));
        }
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        if (map.containsKey(key)) {
            //noinspection ConstantConditions
            return Float.parseFloat(map.get(key));
        }
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (map.containsKey(key)) {
            return Boolean.parseBoolean(map.get(key));
        }
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        return map.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new SimpleEdit();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new RuntimeException("not supported");
    }

    public class SimpleEdit implements Editor {
        private Map<String, String> newMap = new HashMap<>(map);

        @Override
        public Editor putString(String key, @Nullable String value) {
            if (value == null) {
                remove(key);
            } else {
                newMap.put(key, value);
            }
            return this;
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            throw new RuntimeException("not supported");
        }

        @Override
        public Editor putInt(String key, int value) {
            newMap.put(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            newMap.put(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            newMap.put(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            newMap.put(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor remove(String key) {
            newMap.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            newMap.clear();
            return this;
        }

        @Override
        public boolean commit() {
            apply();
            return true;
        }

        @Override
        public void apply() {
            map = newMap;
        }
    }
}
