# show error when not enough memory
```
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
    if (new StatFs(context.getExternalFilesDir(null).getPath()).getFreeBytes() < 100_000_000) {
        throw new RuntimeException("not enough memory to download APK");
    }
}
```

