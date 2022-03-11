[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/de.marmaro.krt.ffupdater)

# Firefox-Updater

Download, install and update these privacy friendly browsers:

- [Brave Private Browser](https://play.google.com/store/apps/details?id=com.brave.browser&hl=en_US)
  ([GitHub Repository](https://github.com/brave/brave-browser))
- [Bromite](https://github.com/bromite/bromite)
- [Firefox Browser](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
  ([GitHub Repository](https://github.com/mozilla-mobile/fenix))
- [Firefox Focus](https://play.google.com/store/apps/details?id=org.mozilla.focus)
  ([GitHub Repository](https://github.com/mozilla-mobile/focus-android))
- [Firefox Klar](https://play.google.com/store/apps/details?id=org.mozilla.klar)
  ([GitHub Repository](https://github.com/mozilla-mobile/focus-android))
- [Firefox Nightly](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
  ([GitHub Repository](https://github.com/mozilla-mobile/fenix))
- [Firefox for Android Beta](https://play.google.com/store/apps/details?id=org.mozilla.firefox_beta)
  ([GitHub Repository](https://github.com/mozilla-mobile/fenix))
- [Iceraven](https://github.com/fork-maintainers/iceraven-browser)
- [Ungoogled Chromium](https://github.com/ungoogled-software/ungoogled-chromium-android)
- [Vivaldi](https://vivaldi.com/download/) [Source Code](https://vivaldi.com/source/)

and:

- [Firefox Lockwise](https://play.google.com/store/apps/details?id=mozilla.lockbox)

FFUpdater will check periodically for updates and will display a notification when an update is available.
This feature itself can be disabled and the check frequency can be changed.

FFUpdater will also automatically download the app updates in the background if the current network is
unmetered and the device has enough storage.

## Security measures

- The certificate fingerprint of every downloaded APK file is checked and validated against an internal
  allowlist. This prevents the installation of malicious app versions that do not originate from the original
  developer. The certificate fingerprints will be displayed in the app and be check by yourself. Some websites
  like apkmirror.com list these fingerprints for every app.
- No unencrypted traffic is used or accepted, which can potentially be modified by attackers. Only HTTPS
  connections.
- Slight risk for a machine-in-the-middle-attack: FFUpdater trusts HTTPS certificates which are installed by
  the user (called "user certificates"). This is necessary for AdGuard (see
  [GitHub-Issue](https://github.com/Tobi823/ffupdater/issues/37)). Be careful which certificates you install.
  A malicious certificate can prevent FFUpdater finding new updates.

Feel free to check my source code ;)

## Download sources for applications

The applications are downloaded from these locations:

- Brave Private Browser: <https://api.github.com/repos/brave/brave-browser/releases/latest>
- Bromite: <https://api.github.com/repos/bromite/bromite/releases/latest>
- Firefox Browser: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.release.latest>
- Firefox Focus: <https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest>
- Firefox Klar: <https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest>
- Firefox Lockwise: <https://api.github.com/repos/mozilla-lockwise/lockwise-android/releases/latest>
- Firefox Nightly: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest>
- Firefox for Android
  Beta: <https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest>
- Iceraven: <https://api.github.com/repos/fork-maintainers/iceraven-browser/releases/latest>
- Ungoogled Chromium: <https://api.github.com/repos/ungoogled-software/ungoogled-chromium-android/releases>
- Vivaldi: <https://vivaldi.com/download/>

## Other distribution channels

The main distribution method of FFUpdater remains F-Droid - this will not change. But you can use the APK
files or the F-Droid repository to quickly install fixed versions.

You need to uninstall FFUpdater every time you switch between F-Droid version and my version.

I don't know if F-Droid shows an update notification when you have my version installed. Maybe you need to
manually check for updates in the F-Droid app.

My versions will be signed with:

```
DN: CN=Tobi823, O=FFUpdater, ST=Germany, C=DE
SHA-256 digest: f4e642bb85cbbcfd7302b2cbcbd346993a41067c27d995df492c9d0d38747e62
SHA-1 digest: b432e9eb74512c0fa58c1ec45912a670f8dfa8e9
MD5 digest: 78a34e36cedd844954726f1e2076642c
```

The official F-Droid Android client sometimes has problems accessing the APK file from my F-Droid repository.
But other clients like "Foxy Droid" work fine. Does anyone know the reason for this? Does the official F-Droid
client has an internal cache and therefore needs more time before it can access a new version from my
repository?

### APK files on GitHub

The APK files are available on [GitHub](https://github.com/Tobi823/ffupdater/releases) and
[GitLab](https://gitlab.com/Tobiwan/ffupdater_gitlab/-/tags).

### F-Droid repository

<a href="fdroidrepos://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo?fingerprint=6E4E6A597D289CB2D4D4F0E4B792E14CCE070BDA6C47AF4918B342FA51F2DC89">
Click here to add the FFUpdater repository to your F-Droid client.</a>

If the link is not clickable, you can use this help
website [https://tobi823.github.io/ffupdaterrepo.html](https://tobi823.github.io/ffupdaterrepo.html).

Repository address: `https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo`

Fingerprint: `6E4E6A597D289CB2D4D4F0E4B792E14CCE070BDA6C47AF4918B342FA51F2DC89`

[<img src="https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo/assets/qrcode.png" alt="F-Droid repository link" height="120">](https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo/assets/qrcode.png)

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
- I don't optimize the APK file with minifyEnabled and shrinkResources because it makes the app harder to
  debug
- Tor Browser / Orbot: I will not support them because they can be installed with F-Droid.
  (Go to Settings > Repositories > Enable "Guardian Project Official Releases".)
  I don't feel confident enough that I can install and update the Tor Browser / Orbot securely
  (because I think there is much more at stake than with other browsers).
- Please reopen FFUpdater after moving it to the internal/external storage.

## Git repositories

- Main repository: https://notabug.org/Tobiwan/ffupdater
- Mirror repository on Github: https://github.com/Tobi823/ffupdater
- Mirror repository on Gitlab: https://gitlab.com/Tobiwan/ffupdater_gitlab

## 3rd-party libraries

- [AndroidX](https://developer.android.com/jetpack/androidx) by Google for UI
- [Material Components](https://github.com/material-components/material-components-android) by Google for UI (
  Apache 2.0)
- [Gson](https://github.com/google/gson) by Google for parsing API responses (Apache 2.0)
- [Shared Preferences Mock](https://github.com/IvanShafran/shared-preferences-mock) by Ivan Shafran for
  testing SharedPreferences (MIT)
- [Kotlin](https://github.com/JetBrains/kotlin) by Jetbrains for programming language Kotlin
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) by Jetbrains for concurrency (Apache 2.0)
- [JUnit 4](https://github.com/junit-team/junit4) for testing (EPL-1.0)
- [MockK](https://mockk.io/) for testing (Apache 2.0)
- [Hamcrest](https://github.com/hamcrest/JavaHamcrest) by Joe Walnes, Nat Pryce and Steve Freeman for
  testing (BSD)
- [Hamcrest Date](https://github.com/eXparity/hamcrest-date) by Stewart Bissett for testing with java.time (
  BSD-3)
- Partially copy and modify [Crasher by James Fenn (Apache 2.0)](https://github.com/fennifith/Crasher) to the
  FFUpdater code base for crash reports
- [G00fY2/version-compare](https://github.com/G00fY2/version-compare) for comparing versions of installed and
  available apps (Apache 2.0)

## My motivation / Project goals

[Goals](GOALS.md)

## How to contribute

[How to contribute](HOW_TO_CONTRIBUTE.md)

## Deprecated browsers#

### Styx Browser

02.01.2022 https://github.com/Tobi823/ffupdater/issues/101

Styx is only temporarily removed from Google Play:

> Sorry for the premature end of Styx. But in the future the Styx browser will appear again. Currently styx
> is migrated to Fulguris base. When this is completed then Styx will return as well as in the Play Store.

30.12.2021: https://github.com/Tobi823/ffupdater/issues/101

It seems that @jamal2362 does not maintain the Styx browser any longer:

- The GitHub repository (https://github.com/jamal2362/Styx) is deleted or private.
- Styx is removed from Google Play (https://play.google.com/store/apps/details?id=com.jamal2367.styx).

Due to the deleted GitHub repository, Styx cannot be downloaded by FFUpdater. And even if - end users should
not use a browser that is no longer updated.

### Kiwi Browser

17.04.2021: https://github.com/Tobi823/ffupdater/issues/35
Answer from the Kiwi developer:

> It's actually quite simple, Kiwi earns money for every search it forwards to Yahoo or Microsoft Bing.

It seems that the developers have to forward search requests to their servers and then to the search engine in
order to get paid by the search engine.

> The parameters and integration method are defined by the search engines themselves, we don't have our words
> at all how the integration is done.

> They [Yahoo or Microsoft Bing] have a standard guide on how to integrate, either you follow this guide, or
> you don't work with them.

I guess it's fine. Although i don't like it, i understand that the money for the app development has to come
from somewhere (even Firefox is paid to use "Google" as the default search engine).

But I think that Kiwi should not be managed by FFUpdater because this browser has additional usability
features and no additional privacy features. FFUpdater is about privacy and not usability.

### Firefox Lite

02.12.2021: End of support for Firefox
Lockwise https://support.mozilla.org/en-US/kb/end-of-support-firefox-lockwise
> Mozilla will end support for the Firefox Lockwise app on Android and iOS, effective December 13, 2021.

14.03.2021: The latest release of Firefox Lite is not longer signed and thereby can't be used to upgrade an
existing Firefox Lite installation. Moreover the developers haven't responded to the Github issue "[BUG]
Unsigned apk ?" from 30.01.2021
https://github.com/mozilla-mobile/FirefoxLite/issues/5353. And Firefox Lite will only receive bug fixes in the
future:
> Firefox Lite is currently in Maintenance Mode. No active feature is being done on the product.
> Older Pull Requests and Issues have been marked with the archived label and have been closed.
> However, if you feel an issue is critical enough to be re-opened, please leave a note on the issue
> with an explanation.

30.06.2021: Firefox Lite is removed from Google Play and Galaxy Store, its repository is
archived (https://github.com/mozilla-mobile/FirefoxLite) and development stopped.

> Effective June 30, 2021, this app will no longer receive security or other updates. Download the official
> Firefox Android app now for a fast, private & safe web browser.

These are the reasons why I will remove Firefox Lite.

### Fennec

16.05.2020: Mozilla wants to migrate from Fennec to Fenix. Fennec Beta and Fennec Nightly are already
end-of-life and Fennec Release will be soon.

27.07.2020: 68.11.0 is the last released update for Fennec version, all Fennec browsers are deprecated.

> Fennec is being replaced by our new state-of-the-art mobile browser codenamed "Fenix". We're slowly
> migrating users in order to make sure the experience is as painless and as enjoyable as possible. We
> started to migrate users who were using Fennec Nightly in January (bug 1608882). It took us several weeks
> to be sure of the result and to finally offer Fenix Nightly to all users using Fennec Nightly. Another
> few weeks later, we repeated the same process with Fennec Beta (bug 1614287). Fenix Beta has been offered
> to the whole Fennec Beta population on April 22nd. We're planning to do the same with Fennec Release
> sometimes this year. The schedule is still to be determined.

> The Google Play Store[1] has a lot of nice features, but it's still pretty basic whenever a software
> publisher wants to slowly migrate users. Once a migration is started, we can't provide any Fennec updates
> to the population who wasn't offered Fenix, yet. I can say this restriction is painful to manage for
> Android developers, Mozilla included. Because of it, we had to stop shipping Fennec Nightly/Beta APKs at
> the beginning of each migration. This explains the dates of the last builds. At the same time, we stopped
> building Fennec Nightly/Beta because it enabled us to save technical resources[2] as well as people's
> time[3].

https://bugzilla.mozilla.org/show_bug.cgi?id=1627518

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
