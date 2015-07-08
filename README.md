## Firefox-Updater

Polls Mozilla's FTP Server for latest "Firefox for Android" release, downloads
it and -- after comparing the versionCode -- installs it (if needed).

Currently the app is an activity with a two-button GUI. Checks are made when
the "I am feeling lucky"-button is tapped. The other downloads the last version
known on release date. However, the plan is to move this actually to an Android
service to perform periodical update checks.

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
