[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/app/de.marmaro.krt.ffupdater)

# Firefox-Updater

Installs and updates the following browsers:

Browsers from Mozilla or based on Firefox:

- [Fennec F-Droid](https://f-droid.org/packages/org.mozilla.fennec_fdroid/)
- [Firefox Browser](https://play.google.com/store/apps/details?id=org.mozilla.firefox),
  [Firefox for Android Beta](https://play.google.com/store/apps/details?id=org.mozilla.firefox_beta),
  [Firefox Nightly](https://play.google.com/store/apps/details?id=org.mozilla.firefox)
  ([GitHub Repository](https://github.com/mozilla-mobile/fenix))
- [Firefox Focus](https://play.google.com/store/apps/details?id=org.mozilla.focus), Firefox Focus Beta,
  [Firefox Klar](https://play.google.com/store/apps/details?id=org.mozilla.klar)
  ([GitHub Repository](https://github.com/mozilla-mobile/focus-android))
- [Mull Browser](https://divestos.org/index.php?page=our_apps)
  ([GitLab Repository](https://gitlab.com/divested-mobile/mull-fenix))
- [Iceraven](https://github.com/fork-maintainers/iceraven-browser)
- [Tor Browser](https://www.torproject.org/download),
  [Tor Browser Alpha](https://www.torproject.org/download/alpha/)

Good privacy browsers:

- [Mulch](https://divestos.org/pages/our_apps#mull)
  , [Mulch System WebView](https://divestos.org/pages/our_apps#mull)

Browser which are better than Google Chrome:

- [Brave Private Browser](https://play.google.com/store/apps/details?id=com.brave.browser&hl=en_US),
  [Brave Browser (Beta)](https://play.google.com/store/apps/details?id=com.brave.browser_beta&gl=US),
  [Brave Browser (Nightly)](https://play.google.com/store/apps/details?id=com.brave.browser_nightly&gl=US)
  ([GitHub Repository](https://github.com/brave/brave-browser))
- [Cromite](https://github.com/uazo/cromite)
- [Chromium](https://www.chromium.org/chromium-projects/)
- [DuckDuckGo Browser](https://github.com/duckduckgo/Android)
- [Kiwi Browser Next](https://github.com/kiwibrowser/src.next) ([Incomplete Source Code](https://github.com/kiwibrowser/src.next/issues/1028))
- [Vivaldi](https://vivaldi.com/download/) ([Incomplete Source Code](https://vivaldi.com/source/))
- [Thorium](https://github.com/Alex313031/Thorium-Android)

Other applications:

- [FairEmail](https://github.com/M66B/FairEmail)
- [K-9 Mail / Thunderbird Android](https://github.com/k9mail/k9mail.app)
- [Orbot](https://github.com/guardianproject/orbot)

FFUpdater checks for updates in the background and downloads them as well. Apps can be updated without user
interaction with:

- Android 12 or higher
- rooted smartphone
- [Shizuku](https://shizuku.rikka.app/) / [Sui](https://github.com/RikkaApps/Sui) with Android 6 or higher

## Thanks

## FAQ

- By clicking on the "i"-Icon, you can see the time of the last successful background update check.
- Firefox Nightly: Replace the minutes with 'xx' because FFUpdater can only access the start time of the build
  and not the version name of the app update (finish time). The builds always starts at 5:00 and 17:00 and
  usually takes a few minutes.
- Please reopen FFUpdater after moving it to the internal/external storage.

## How to contribute

You can improve the translation on [Weblate](https://hosted.weblate.org/projects/ffupdater). Current progress:
[<img align="right" src="https://hosted.weblate.org/widgets/ffupdater/-/287x66-white.png" alt="Get involved in translating FFUpdater" />](https://hosted.weblate.org/engage/ffupdater/?utm_source=widget)

[![Translation status](https://hosted.weblate.org/widgets/ffupdater/-/multi-auto.svg)](https://hosted.weblate.org/engage/ffupdater/?utm_source=widget)

Your translation contribution will be acknowledged in every release changelog.

For advanced users: [How to contribute](HOW_TO_CONTRIBUTE.md)

## Source Code Contributors

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="http://www.linestarve.com/"><img src="https://avatars.githubusercontent.com/u/2261204?v=4?s=100" width="100px;" alt="Wolfgang Faust"/><br /><sub><b>Wolfgang Faust</b></sub></a><br /><a href="#code-wolfgang42" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/gnuhead-chieb"><img src="https://avatars.githubusercontent.com/u/41156994?v=4?s=100" width="100px;" alt="gnuhead-chieb"/><br /><sub><b>gnuhead-chieb</b></sub></a><br /><a href="#translation-gnuhead-chieb" title="Translation">🌍</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

## Additional documentation

[Security measures](docs/security_measures.md)

[Download sources](docs/download_sources.md)

[F-Droid repository / APK files / other distribution channels](docs/other_distribution_channels.md)

[3rd-party libraries](docs/3rd_party_libraries.md)

[Deprecated browsers](docs/deprecated_browsers.md)

[Maintainer](docs/maintainer.md)

[My goals](GOALS.md)

## Git repositories

- Main repository: https://github.com/Tobi823/ffupdater
- Mirror repository on notabug.org: https://notabug.org/Tobiwan/ffupdater
- Mirror repository on Gitlab: https://gitlab.com/Tobiwan/ffupdater_gitlab

## License

````
FFUpdater -- Updater for privacy friendly browser
Copyright (C) 2019-2023 Tobias Hellmann https://github.com/Tobi823
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
