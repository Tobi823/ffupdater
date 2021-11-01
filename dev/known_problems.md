- On LineageOS 18.1, the DownloadManager could forget to add the file suffix (".apk") to the downloaded file (
  notabug.org#79)
- On some Amazon devices, an APK file must be downloaded to the "ExternalFilesDir" or FFUpdater is not able to
  access the
  file `Permission Denial: reading com.android.providers.downloads.DownloadProvider uri ... requires android.permission.ACCESS_ALL_DOWNLOADS, or grantUriPermission()` (
  GitHub#86)
- `packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)` does not work on Android 9/10, but on
  Android 11/12