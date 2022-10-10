[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/de.marmaro.krt.ffupdater)

# Firefox-Updater

Installs and updates the following browsers:

Browsers from Mozilla or based on Firefox:

- [Fennec F-Droid](https://f-droid.org/packages/org.mozilla.fennec_fdroid/)
- [Firefox Browser](https://play.google.com/store/apps/details?id=org.mozilla.firefox),
  [Firefox for Android Beta](https://play.google.com/store/apps/details?id=org.mozilla.firefox_beta),
  [Firefox Nightly](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
  ([GitHub Repository](https://github.com/mozilla-mobile/fenix))
- [Firefox Focus](https://play.google.com/store/apps/details?id=org.mozilla.focus),
  [Firefox Klar](https://play.google.com/store/apps/details?id=org.mozilla.klar)
  ([GitHub Repository](https://github.com/mozilla-mobile/focus-android))
- [Mull Browser](https://divestos.org/index.php?page=our_apps)
  ([GitLab Repository](https://gitlab.com/divested-mobile/mull-fenix))
- [Iceraven](https://github.com/fork-maintainers/iceraven-browser)
- [Tor Browser](https://www.torproject.org/download)

Good privacy browsers:

- [Bromite](https://github.com/bromite/bromite),
  [Bromite SystemWebView](https://www.bromite.org/system_web_view)
- [Mulch](https://divestos.org/index.php?page=our_apps)

Browser which are better than Google Chrome:

- [Brave Private Browser](https://play.google.com/store/apps/details?id=com.brave.browser&hl=en_US),
  [Brave Browser (Beta)](https://play.google.com/store/apps/details?id=com.brave.browser_beta&gl=US),
  [Brave Browser (Nightly)](https://play.google.com/store/apps/details?id=com.brave.browser_nightly&gl=US)
  ([GitHub Repository](https://github.com/brave/brave-browser))
- [Chromium](https://www.chromium.org/chromium-projects/)
- [Kiwi Browser Next](https://github.com/kiwibrowser/src.next)
- [Vivaldi](https://vivaldi.com/download/) ([Incomplete Source Code](https://vivaldi.com/source/))

Other applications:

- [Orbot](https://github.com/guardianproject/orbot)

FFUpdater checks for updates in the background and downloads them as well. On Android 12+ or root devices,
FFUpdater can also update the apps without user interaction.

## How to contribute

You can improve the translation on [Weblate](https://hosted.weblate.org/projects/ffupdater). Current progress:
[<img align="right" src="https://hosted.weblate.org/widgets/ffupdater/-/287x66-white.png" alt="Get involved in translating FFUpdater" />](https://hosted.weblate.org/engage/ffupdater/?utm_source=widget)

[![Translation status](https://hosted.weblate.org/widgets/ffupdater/-/multi-auto.svg)](https://hosted.weblate.org/engage/ffupdater/?utm_source=widget)

For advanced users: [How to contribute](HOW_TO_CONTRIBUTE.md)

## Security measures

- The signature fingerprint of every downloaded APK file is validated against an internal allowlist. This
  prevents the installation of malicious apps that do not originate from the original developers.
- Only HTTPS connections are used because unencrypted HTTP traffic can be manipulated.
- Only system certificate authorities are trusted. But this can be disabled in the settings to allow other
  apps to inspect the application's network traffic.
- Prevent command injection in the RootInstaller.kt by validating and sanitizing commands.
- Git commits will be signed with the GPG key
  CE72BFF6A293A85762D4901E426C5FB1C7840C5F [public key](dev/signatures/ffupdater_gpg_public.key)

## Download sources for applications

The applications are downloaded from these locations:

- Brave Private Browser, Brave Browser (Beta), [Brave Browser (
  Nightly): <https://api.github.com/repos/brave/brave-browser/releases/latest>
- Bromite, Bromite SystemWebView: <https://api.github.com/repos/bromite/bromite/releases/latest>
- Chromium: <https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Android%2F>
- Fennec F-Droid: <https://f-droid.org/repo/org.mozilla.fennec_fdroid_XXXXXXX.apk>
- Firefox Browser: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.release.latest>
- Firefox Focus, Firefox
  Klar: <https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest>
- Firefox Lockwise: <https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest>
- Firefox Nightly: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest>
- Firefox for Android
  Beta: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest>
- Iceraven: <https://api.github.com/repos/fork-maintainers/iceraven-browser/releases/latest>
- Kiwi Browser Next: <https://api.github.com/repos/kiwibrowser/src.next/releases>
- Orbot: <https://api.github.com/repos/kiwibrowser/guardianproject/orbot>
- Tor Browser: <https://www.torproject.org/download>
- Vivaldi: <https://vivaldi.com/download/>

## Other distribution channels

The main distribution method of FFUpdater remains F-Droid - this will not change. But you can use the APK
files or the F-Droid repository to quickly install fixed versions.

You need to uninstall FFUpdater every time you switch between F-Droid version and my version. F-Droid will not
show you new updates if you still uses the version from GitHub/GitLab.

My versions will be signed with this [certificate](dev/signatures/apk_signature.txt).

The official F-Droid Android client sometimes has problems accessing the APK file from my F-Droid repository.
But other clients like "Foxy Droid" work fine.

### APK files on GitHub

The APK files are available on [GitHub](https://github.com/Tobi823/ffupdater/releases) and
[GitLab](https://gitlab.com/Tobiwan/ffupdater_gitlab/-/tags).

### F-Droid repository

Repository address: `https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo`

Fingerprint: `6E4E6A597D289CB2D4D4F0E4B792E14CCE070BDA6C47AF4918B342FA51F2DC89`

[![Add](https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo/assets/qrcode.png)](https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo/assets/qrcode.png)

[Add the repository to F-Droid](https://tobi823.github.io/ffupdaterrepo.html)

[How to Add a Repo to F-Droid](https://f-droid.org/en/tutorials/add-repo/)

It seems that this F-Droid repository is sometimes a little buggy. If F-Droid fails to download FFUpdater, try
to install the version from the official F-Droid repository first.

On the app page under the item "Versions" you can see from which repository (my *FFUpdater*
repository or the official *F-Droid* repository) the app version was installed

## FAQ

- By clicking on the "i"-Icon, you can see the time of the last successful background update check.
- Firefox Nightly: Replace the minutes with 'xx' because FFUpdater can only access the start time of the build
  and not the version name of the app update (finish time). The builds always starts at 5:00 and 17:00 and
  usually takes a few minutes.
- Please reopen FFUpdater after moving it to the internal/external storage.

## Git repositories

- Main repository: https://github.com/Tobi823/ffupdater
- Mirror repository on notabug.org: https://notabug.org/Tobiwan/ffupdater
- Mirror repository on Gitlab: https://gitlab.com/Tobiwan/ffupdater_gitlab

## 3rd-party libraries

- [AndroidX](https://developer.android.com/jetpack/androidx) by Google (Apache 2.0): *user interface*
- [Material Components](https://github.com/material-components/material-components-android) by Google (Apache
  2.0): *user interface*
- [Gson](https://github.com/google/gson) by Google (Apache 2.0): *parsing network responses to GSON*
- [Shared Preferences Mock](https://github.com/IvanShafran/shared-preferences-mock) by Ivan Shafran (MIT): *
  testing SharedPreferences*
- [Kotlin](https://github.com/JetBrains/kotlin) by Jetbrains: *programming language Kotlin*
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) by Jetbrains (Apache 2.0): *concurrency*
- Partially copy and modify [Crasher](https://github.com/fennifith/Crasher) by James Fenn (Apache 2.0): *crash
  reports*
- [version-compare](https://github.com/G00fY2/version-compare) by Thomas Wirth (Apache 2.0): *compare versions
  of installed and available apps*
- [OkHttp](https://square.github.io/okio/) by Square, Inc (Apache 2.0): *download files and make network
  requests*
- [Kotlin coroutines await extension for OkHttp3](https://github.com/gildor/kotlin-coroutines-okhttp) by
  Andrey Mischenko (Apache 2.0): *add async/await support to OkHttp*
- [JUnit 5](https://junit.org/junit5/) by The JUnit Team (MIT): *software testing*
- [android-junit5](https://github.com/mannodermaus/android-junit5) by Marcel Schnelle (Apache 2.0): *use
  JUnit5 software tests with Android*
- [MockK](https://mockk.io/) (Apache 2.0): *for easier software testing*
- Partially copy and
  modify [Root app installer](https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/installer/RootInstaller.kt)
  by Aurora Store / Rahul Patel (GPL): *for installing/updating apps without user interaction*
- [libsu](https://github.com/topjohnwu/libsu) by John Wu (Apache 2.0): *for executing root commands*

## My motivation / Project goals

[Goals](GOALS.md)

## Deprecated browsers

### Mozilla Lockwise

27.06.2022: Mozilla Lockwise is removed from FFUpdater. https://github.com/Tobi823/ffupdater/issues/194

> Mozilla ended support for the Firefox Lockwise app on Android and iOS, effective December 13, 2021.
> https://support.mozilla.org/en-US/kb/end-of-support-firefox-lockwise

### Styx Browser

02.01.2022: Styx Browser is temporary removed from FFUpdater.

> Sorry for the premature end of Styx. But in the future the Styx browser will appear again. Currently styx
> is migrated to Fulguris base. When this is completed then Styx will return as well as in the Play Store.
> https://github.com/Tobi823/ffupdater/issues/101

### Firefox Lite

02.12.2021: Firefox Lite is removed from FFUpdater.

> Mozilla will end support for the Firefox Lite browser on June 30, 2021.
> https://support.mozilla.org/en-US/kb/end-support-firefox-lite

### Fennec

27.07.2020: Firefox Release/Beta/Nightly based on Fennec are removed from FFUpdater.

> 68.11.0 is the last released update for Fennec version, all Fennec browsers are deprecated.
> https://bugzilla.mozilla.org/show_bug.cgi?id=1627518

## Build app

Use Android Studio to clone and run the app. Nothing special needs to be done.

## Maintainer:

### Tobiwan (now)

### Boris Kraut (https://gitlab.com/krt/ffupdater, until April 2019)

> Since I left F-Droid (and Android/Smartphones) about a year ago, I am looking for a new maintainer to take
> over. Unfortunately the upstream issue I opened years ago is still not solved in 2019. While Fennec F-Droid
> is back in the mainline repo and other binary repos do serve Firefox, some might still prefer this updater.
> So as I said: Maintainers welcome. The main task should be to test the last few merge requests (especially
> the background update stuff) and release a new version.
> **New Maintainer: https://notabug.org/Tobiwan/ffupdater**

## License

````
FFUpdater -- Updater for privacy friendly browser
Copyright (C) 2019-2021 Tobias Hellmann https://github.com/Tobi823
Copyright (C) 2015-2019 Boris Kraut <krt@nurfuerspam.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
````
