[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/de.marmaro.krt.ffupdater)

## Firefox-Updater

An Android app to install and update:
 - [Firefox Browser / Fennec Release](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
 - [Firefox Beta / Fennec Beta](https://play.google.com/store/apps/details?id=org.mozilla.firefox_beta)
 - [Firefox Nightly / Fennec Nightly](https://play.google.com/store/apps/details?id=org.mozilla.fennec_aurora)
 - [Firefox Focus](https://play.google.com/store/apps/details?id=org.mozilla.focus)
 - [Firefox Klar](https://play.google.com/store/apps/details?id=org.mozilla.klar)
 - [Firefox Lite](https://play.google.com/store/apps/details?id=org.mozilla.rocket)
 - [Firefox Preview / Fenix](https://play.google.com/store/apps/details?id=org.mozilla.fenix)
 on your device without using the Google Play Store.

The app can check periodically for updates and will display a notification when an update is available. 

## Technical detail

### Fennec Release, Fennec Beta and Fennec Nightly 

The Mozilla API will be queried:
 - https://archive.mozilla.org/pub/mobile/releases/ for latest version names
 - https://download.mozilla.org/?product=%s&os=%s&lang=multi to download the  app
   - first parameter: fennec-latest, fennec-beta-latest or fennec-nightly-latest
   - second parameter: android or android-x86

### Firefox Focus, Firefox Klar, Firefox Lite, Fenix

The Github API will be queried:
 - https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest for Firefox Focus and Firefox Klar
 - https://api.github.com/repos/mozilla-tw/FirefoxLite/releases/latest for Firefox Lite
 - https://api.github.com/repos/mozilla-mobile/fenix/releases/latest for Fenix
 
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
