[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/de.marmaro.krt.ffupdater)

[Boris Kraut:](https://gitlab.com/krt)
> Since I left F-Droid (and Android/Smartphones) about a year ago, I am looking for a new maintainer to take over. Unfortunately the upstream issue I opened 3 years ago is still not solved in 2018. While Fennec F-Droid is back in the mainline repo and other binary repos do serve Firefox, some might still prefer this updater. So as I said: Maintainers welcome. The main task should be to test the last few merge requests (especially the background update stuff) and release a new version

## Firefox-Updater

An Android app to install and/or update:
 - [Firefox](https://www.mozilla.org/de/firefox/mobile/)
 - [Firefox Beta](https://www.mozilla.org/de/firefox/channel/android/)
 - [Firefox Nightly](https://www.mozilla.org/de/firefox/channel/android/)
 - [Firefox Klar](https://www.mozilla.org/de/firefox/mobile/#klar)
 - [Firefox Focus](https://www.mozilla.org/en-US/firefox/mobile/#focus)

for Android on your device, without using the Google Play Store.

Shows the currently installed Firefox versions (if available) and allows to query Mozilla's [releases directory site](https://archive.mozilla.org/pub/mobile/releases/) and the [GitHub-API](https://api.github.com/repos/mozilla-mobile/focus-android/releases/latest) of [mozilla-mobile/focus-android](https://github.com/mozilla-mobile/focus-android) for the latest Firefox releases. It also lets a user start downloading the latest release version.

Every 4 hours the app checks in the background for new releases (power-efficient with [setInexactRepeating](https://developer.android.com/reference/android/app/AlarmManager.html#setInexactRepeating(int,%20long,%20long,%20android.app.PendingIntent))). If a new release is available, a notification will be shown. 

Currently the app is an activity with a two-button GUI. Checks for and displays the most current available versions when the "Check available version"-button is tapped. The "Download" button starts an intent to download the last version from a URL described in Mozilla's [README.txt](https://archive.mozilla.org/pub/mobile/releases/latest/README.txt).

## History

Since Mozilla [shut down their FTP server on 2015-08-05](https://blog.mozilla.org/it/2015/07/27/product-delivery-migration-what-is-changing-when-its-changing-and-the-impacts/), non-playstore updates of Firefox have to be done by third-party apps such as this one.


Mozilla now uses a uniform URL to point to the latest release, see [their README](https://archive.mozilla.org/pub/mobile/releases/latest/README.txt) for details. The current URL for Android is: https://download.mozilla.org/?product=fennec-latest&os=android&lang=multi

[Boris Kraut](https://gitlab.com/krt) opened a ticket about non-playstore updates with Mozilla in 2015, as did others:

- [ ] https://bugzilla.mozilla.org/show_bug.cgi?id=1192279
- [x] https://bugzilla.mozilla.org/show_bug.cgi?id=1220773

## License

````
FFUpdater -- a simple Android app to update Firefox.
Copyright (C) 2015-2018 Boris Kraut <krt@nurfuerspam.de>

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
