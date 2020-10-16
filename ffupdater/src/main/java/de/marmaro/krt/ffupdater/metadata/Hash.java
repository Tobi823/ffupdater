package de.marmaro.krt.ffupdater.metadata;

public class Hash {
    private final Type type;
    private final String hexString;

    public Hash(Type type, String hexString) {
        this.type = type;
        this.hexString = hexString;
    }

    public Type getType() {
        return type;
    }

    public String getHexString() {
        return hexString;
    }

    public enum Type {
        SHA256
    }
}
