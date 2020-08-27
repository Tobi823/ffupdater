[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/de.marmaro.krt.ffupdater)

## Firefox-Updater

Download, install and update these browsers from Mozilla:
 - [Firefox Browser](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
 - [Firefox for Android Beta](https://play.google.com/store/apps/details?id=org.mozilla.firefox_beta)
 - [Firefox Nightly](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
 - [Firefox Focus](https://play.google.com/store/apps/details?id=org.mozilla.focus)
 - [Firefox Klar](https://play.google.com/store/apps/details?id=org.mozilla.klar)
 - [Firefox Lite](https://play.google.com/store/apps/details?id=org.mozilla.rocket)
 - [Firefox Lockwise](https://play.google.com/store/apps/details?id=mozilla.lockbox)

FFUpdater will check periodically for updates and will display a notification when an update is available. This feature itself can be disabled and the check frequency can be changed.

Security measures:
 - only HTTPS connections
 - check fingerprint of the downloaded file
 - check fingerprint of the installed app

You can find the APK certificate fingerprints on multiple website - e.g. apkmirror.com
I did my best to make the app as secure as possible - feel free to double-check it in the source code.

The applications are downloaded from these locations:
 - Firefox Browser: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.firefox.fennec-production.latest>
 - Firefox for Android Beta: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.firefox.fennec-beta.latest/arm64-v8a>
 - Firefox Nightly: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.firefox.nightly.latest>
 - Firefox Focus/Klar: <https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest>
 - Firefox Lite: <https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest>
 - Firefox Lockwise: <https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest>
 
Limitations:
 - FFUpdater can't detect external installations or updates of Firefox Browser, Firefox for Android Beta, Firefox Nightly, Firefox Focus and Firefox Klar. If you install or update one of these apps with the Google Play Store, FFUpdater assumes that this app is outdated and prompts you for an update. You can disable the update check for an app in settings > excluded applications.

## Deprecated browsers

### Fennec
16.05.2020: Mozilla wants to migrate from Fennec to Fenix. Fennec Beta and Fennec Nightly are already end-of-life and Fennec Release will be soon.

>Fennec is being replaced by our new state-of-the-art mobile browser codenamed "Fenix". We're slowly migrating users in order to make sure the experience is as painless and as enjoyable as possible. We started to migrate users who were using Fennec Nightly in January (bug 1608882). It took us several weeks to be sure of the result and to finally offer Fenix Nightly to all users using Fennec Nightly. Another few weeks later, we repeated the same process with Fennec Beta (bug 1614287). Fenix Beta has been offered to the whole Fennec Beta population on April 22nd. We're planning to do the same with Fennec Release sometimes this year. The schedule is still to be determined.

 >The Google Play Store[1] has a lot of nice features, but it's still pretty basic whenever a software publisher wants to slowly migrate users. Once a migration is started, we can't provide any Fennec updates to the population who wasn't offered Fenix, yet. I can say this restriction is painful to manage for Android developers, Mozilla included. Because of it, we had to stop shipping Fennec Nightly/Beta APKs at the beginning of each migration. This explains the dates of the last builds. At the same time, we stopped building Fennec Nightly/Beta because it enabled us to save technical resources[2] as well as people's time[3].

https://bugzilla.mozilla.org/show_bug.cgi?id=1627518

## Build app

### Android Studio

#### From an existing project

 1. File
 2. New
 3. Project from Version Control...
 4. Git
 5. URL: https://notabug.org/Tobiwan/ffupdater.git
 6. Clone
 7. Wait for Android Studio to import project
 8. Build
 9. Build Bundle(s) / APK(s)
 10. Build APK(s)
 11. Wait
 12. Click in the bottom right on "locate"

#### From "Welcome to Android Studio" screen

 1. Check out project from Version Control
 2. Git
 3. URL: https://notabug.org/Tobiwan/ffupdater.git
 4. Clone
 5. "Would you like to open it?" Yes
 6. Wait for Android Studio to import project
 7. Build
 8. Build Bundle(s) / APK(s)
 9. Build APK(s)
 10. Wait
 11. Click in the bottom right on "locate"

### Gradle

 1. `git clone https://notabug.org/Tobiwan/ffupdater.git`
 2. `cd ffupdater`
 3. create the file `local.properties` with the path to your Android SDK - for example:
 ````
 sdk.dir=C\:\\Users\\Tobias\\AppData\\Local\\Android\\Sdk
 ````
 4. `gradlew assembleDebug`
 5. `cd ffupdater/build/outputs/apk/debug`
 6. `ffupdater-debug.apk` is the generated APK file


## Mirrors
 - [Notabug.org](https://notabug.org/Tobiwan/ffupdater) ([Account](https://notabug.org/Tobiwan))
 - [Github](https://github.com/Tobi823/ffupdater) ([Account](https://github.com/Tobi823))
 - [Gitlab](https://gitlab.com/Tobiwan/ffupdater) ([Account](https://gitlab.com/Tobiwan))

## Technical detail

### Fennec Release

The Mozilla API will be queried:
 - https://archive.mozilla.org/pub/mobile/releases/ for latest version names
 - https://download.mozilla.org/?product=%s&os=%s&lang=multi to download the  app
   - first parameter: fennec-latest, fennec-beta-latest or fennec-nightly-latest
   - second parameter: android or android-x86

### Firefox Focus, Firefox Klar, Firefox Lite, Fenix

The Github API will be queried:
 - https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest for Firefox Focus and Firefox Klar
 - https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest for Firefox Lite
 - https://api.github.com/repos/mozilla-mobile/firefox/releases/latest for Fenix
 
If .../releases/latest returns a release without assets (APK files for download), then fetch all releases and search
for the latest release with assets.

## History

Since Mozilla [shut down their FTP server on 2015-08-05](https://blog.mozilla.org/it/2015/07/27/product-delivery-migration-what-is-changing-when-its-changing-and-the-impacts/), non-"Google Play Store" updates of Firefox have to be done by third-party apps such as this one.


Mozilla now uses a uniform URL to point to the latest release, see [their README](https://archive.mozilla.org/pub/mobile/releases/latest/README.txt) for details. The current URL for Android is: https://download.mozilla.org/?product=fennec-latest&os=android&lang=multi

I opened a ticket about non-"Google Play Store" updates with Mozilla in 2015, as did others:

- [ ] https://bugzilla.mozilla.org/show_bug.cgi?id=1192279
- [x] https://bugzilla.mozilla.org/show_bug.cgi?id=1220773

### Maintainer:

#### Tobiwan (now)

#### Boris Kraut (https://gitlab.com/krt/ffupdater, until April 2019)
> Since I left F-Droid (and Android/Smartphones) about a year ago, I am looking for a new maintainer to take over. Unfortunately the upstream issue I opened years ago is still not solved in 2019. While Fennec F-Droid is back in the mainline repo and other binary repos do serve Firefox, some might still prefer this updater. So as I said: Maintainers welcome. The main task should be to test the last few merge requests (especially the background update stuff) and release a new version.
> **New Maintainer: https://notabug.org/Tobiwan/ffupdater**

## License

````
FFUpdater -- a simple Android app to update Firefox.
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
