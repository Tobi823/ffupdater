package de.marmaro.krt.ffupdater.utils;

public class ParamRuntimeException extends RuntimeException {
    public ParamRuntimeException(String format, Object... args) {
        super(String.format(format, args));
    }
    public ParamRuntimeException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }
}
