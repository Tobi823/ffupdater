## Firefox-Updater

INFO: Mozilla has discontinued FTP service as of 2015-08-05, see the
following blog post for details:

* https://blog.mozilla.org/it/2015/07/27/product-delivery-migration-what-is-changing-when-its-changing-and-the-impacts/

Polls Mozilla's FTP Server for latest "Firefox for Android" release, downloads
it and -- after comparing the versionCode -- installs it (if needed).

Currently the app is an activity with a two-button GUI. Checks are made when
the "I am feeling lucky"-button is tapped. The other downloads the last version
known on release date. However, the plan is to move this actually to an Android
service to perform periodical update checks.

However, Mozilla plans to shutdown the FTP server, so we cannot rely on this
update method. I opened a ticket about non-playstore updates with Mozilla:

https://bugzilla.mozilla.org/show_bug.cgi?id=1192279

Another related issue is tracker at:

https://bugzilla.mozilla.org/show_bug.cgi?id=1220773


Update: Mozilla now uses a uniform URL to point to the latest release, see

https://archive.mozilla.org/pub/mobile/releases/latest/README.txt
https://download.mozilla.org/?product=fennec-latest&os=android&lang=multi

## License

FFUpdater -- a simple Android app to update Firefox.
Copyright (C) 2015 Boris Kraut <krt@nurfuerspam.de>

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
