[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/de.marmaro.krt.ffupdater)

# Firefox-Updater
Download, install and update these privacy friendly browsers:
 - [Brave Private Browser](https://play.google.com/store/apps/details?id=com.brave.browser&hl=en_US)
 - [Firefox Browser](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
 - [Firefox Focus](https://play.google.com/store/apps/details?id=org.mozilla.focus)
 - [Firefox for Android Beta](https://play.google.com/store/apps/details?id=org.mozilla.firefox_beta)
 - [Firefox Klar](https://play.google.com/store/apps/details?id=org.mozilla.klar)
 - [Firefox Nightly](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
 - [Iceraven](https://github.com/fork-maintainers/iceraven-browser)

and:
 - [Firefox Lockwise](https://play.google.com/store/apps/details?id=mozilla.lockbox)

FFUpdater will check periodically for updates and will display a notification when an update is available. This feature itself can be disabled and the check frequency can be changed.

## Security measures:
 - only HTTPS connections
 - check certificate fingerprint of the downloaded file and installed app

You can find the APK certificate fingerprints on multiple website - e.g. apkmirror.com
I did my best to make the app as secure as possible - feel free to double-check it in the source code.

## Download server:
The applications are downloaded from these locations:
 - Firefox Browser: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.release.latest>
 - Firefox for Android Beta: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest>
 - Firefox Nightly: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest>
 - Firefox Focus/Klar: <https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest>
 - Firefox Lockwise: <https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest>
 - Brave Private Browser: <https://api.github.com/repos/brave/brave-browser/releases/latest>
 - Iceraven: <https://api.github.com/repos/fork-maintainers/iceraven-browser/releases/latest>
 
## FAQ:
 - By clicking on the "i"-Icon, you can see the time of the last successful background update check.
 - Firefox Nightly: Replace the minutes with 'xx' because FFUpdater can only access the start time 
 of the build and not the version name of the app update (finish time). 
 The builds always starts at 5:00 and 17:00 and usually takes a few minutes.
 - I don't optimize the APK file with minifyEnabled and shrinkResources because it makes the app 
 harder to debug
 
## Git repositories:
 - Main repository: https://notabug.org/Tobiwan/ffupdater
 - Mirror repository on Github: https://github.com/Tobi823/ffupdater
 - Mirror repository on Gitlab: https://gitlab.com/Tobiwan/ffupdater_gitlab

## 3rd-party libraries:
 - [AndroidX](https://developer.android.com/jetpack/androidx) by Google for UI
 - [Material Components](https://github.com/material-components/material-components-android) by Google for UI (Apache 2.0)
 - [Gson](https://github.com/google/gson) by Google for parsing API responses (Apache 2.0)
 - [Crasher](https://github.com/fennifith/Crasher) by James Fenn for crash reports (Apache 2.0)
 - [Shared Preferences Mock](https://github.com/IvanShafran/shared-preferences-mock) by Ivan Shafran for testing SharedPreferences (MIT)
 - [Kotlin](https://github.com/JetBrains/kotlin) by Jetbrains for programming language Kotlin
 - [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) by Jetbrains for concurrency (Apache 2.0)
 - [JUnit 4](https://github.com/junit-team/junit4) for testing (EPL-1.0)
 - [MockK](https://mockk.io/) for testing (Apache 2.0)
 - [Hamcrest](https://github.com/hamcrest/JavaHamcrest) by Joe Walnes, Nat Pryce and Steve Freeman for testing (BSD)
 - [Hamcrest Date](https://github.com/eXparity/hamcrest-date) by Stewart Bissett for testing with java.time (BSD-3)

## My motivation / Project goals
[Goals](GOALS.md)

## How to contribute
[How to contribute](HOW_TO_CONTRIBUTE.md)

## Deprecated browsers

### Firefox Lite
14.03.2021: The latest release of Firefox Lite is not longer signed and thereby can't be used to
upgrade an existing Firefox Lite installation.
Moreover the developers haven't responded to the Github issue "[BUG] Unsigned apk ?" from 30.01.2021 
https://github.com/mozilla-mobile/FirefoxLite/issues/5353.
And Firefox Lite will only receive bug fixes in the future:
>Firefox Lite is currently in Maintenance Mode. No active feature is being done on the product. 
>Older Pull Requests and Issues have been marked with the archived label and have been closed. 
>However, if you feel an issue is critical enough to be re-opened, please leave a note on the issue 
>with an explanation. 

These are the reasons why I will remove Firefox Lite.

### Fennec
16.05.2020: Mozilla wants to migrate from Fennec to Fenix. Fennec Beta and Fennec Nightly are already end-of-life and Fennec Release will be soon.

>Fennec is being replaced by our new state-of-the-art mobile browser codenamed "Fenix". We're slowly migrating users in order to make sure the experience is as painless and as enjoyable as possible. We started to migrate users who were using Fennec Nightly in January (bug 1608882). It took us several weeks to be sure of the result and to finally offer Fenix Nightly to all users using Fennec Nightly. Another few weeks later, we repeated the same process with Fennec Beta (bug 1614287). Fenix Beta has been offered to the whole Fennec Beta population on April 22nd. We're planning to do the same with Fennec Release sometimes this year. The schedule is still to be determined.

 >The Google Play Store[1] has a lot of nice features, but it's still pretty basic whenever a software publisher wants to slowly migrate users. Once a migration is started, we can't provide any Fennec updates to the population who wasn't offered Fenix, yet. I can say this restriction is painful to manage for Android developers, Mozilla included. Because of it, we had to stop shipping Fennec Nightly/Beta APKs at the beginning of each migration. This explains the dates of the last builds. At the same time, we stopped building Fennec Nightly/Beta because it enabled us to save technical resources[2] as well as people's time[3].

https://bugzilla.mozilla.org/show_bug.cgi?id=1627518

## Build app
Use Android Studio to clone and run the app.
Nothing special needs to be done.

## Maintainer:
### Tobiwan (now)
### Boris Kraut (https://gitlab.com/krt/ffupdater, until April 2019)
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
