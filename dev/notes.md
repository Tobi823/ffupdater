# Git commit signing

```
git config user.email ffupdater@mailbox.org
git config user.signingkey CE72BFF6A293A85762D4901E426C5FB1C7840C5F
git config commit.gpgsign true
```

# Weblate

## Don't translate changelogs

https://docs.weblate.org/en/latest/admin/addons.html?highlight=has%3Alabel#bulk-edit

language:en AND key:changelogs/

read-only

# WorkManager

## List of scheduled WorkManager

`` adb shell dumpsys jobscheduler`` for API 23+
https://stackoverflow.com/questions/55879642/adb-command-to-list-all-scheduled-work-using-workmanager

"force stop" the app will cancel the scheduled update check. But it will reinitialise after WorkManager is
reinitialise (when calling WorkManager.getInstance(context))
Some OS will heavily force-stop apps.

A period work returned "failed" (instead of "success") will not be executed again.

# Development environment

## Commands

get certificate hash / signature

`keytool -list -printcert -jarfile *.apk`

## F-Droid build updates

https://monitor.f-droid.org/builds

## Android Emulator

```
adb shell svc wifi disable
adb shell svc data disable
sleep 5
adb shell svc wifi enable
adb shell svc data disable
```

# Known problems

- On LineageOS 18.1, the DownloadManager could forget to add the file suffix (".apk") to the downloaded file (
  notabug.org#79)
- On some Amazon devices, an APK file must be downloaded to the "ExternalFilesDir" or FFUpdater is not able to
  access the
  file `Permission Denial: reading com.android.providers.downloads.DownloadProvider uri ... requires android.permission.ACCESS_ALL_DOWNLOADS, or grantUriPermission()` (
  GitHub#86)
- `packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)` does not always work on Android 9 or
  later (-> if not successful, use fallback with old method)