package de.marmaro.krt.ffupdater.app.impl;

import com.google.gson.Gson;

public class Test {

    public <T> T find(String a, Class<T> clazz) {
        if (clazz == String.class) {
            return (T) "";
        }
        return new Gson().fromJson(a, clazz);
    }
}
