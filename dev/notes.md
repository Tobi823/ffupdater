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

`apksigner verify -print-certs -v arm64_ChromePublic.apk`

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

# Vanadium

https://gitlab.com/api/v4/projects/grapheneos%2Fplatform_external_vanadium/repository/tags?page=1&per_page=1
-> extract commit.id

https://gitlab.com/api/v4/projects/grapheneos%2Fplatform_external_vanadium/repository/files/prebuilt%2Farm64%2FTrichromeLibrary.apk/raw?ref=d460c77a1d63556491ca9e32c03f5192492a450b

https://gitlab.com/api/v4/projects/grapheneos%2Fplatform_external_vanadium/repository/files/prebuilt%2Farm64%2FTrichromeChrome.apk/raw?ref=d460c77a1d63556491ca9e32c03f5192492a450b

minSdk 29

Zum Abfragen der aktuellen Version:

- commit.id vergleichen
- Version aus Tag-Nachricht extrahieren
- vielleicht Android lastUpdateTime

103.0.5060.71 506007136 org.chromium.chrome

app.vanadium.trichromelibrary 103.0.5060.71 506007134

lastUpdateTime wird geändert, selbst wenn die gleiche Version rüberinstalliert wird
(vielleicht als Fallback, falls keine Version hinterlegt wurde? Lieber nicht)

# Links

https://privacytests.org/android.html

