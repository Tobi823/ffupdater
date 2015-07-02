## Firefox-Updater

Polls Mozilla's FTP Server for latest "Firefox for Android" release, downloads
it and -- after comparing the versionCode -- installs it (if needed).

Currently the app is an activity with a one-button GUI. Checks are made when
the button is tapped. However, the plan is to move this actually to an Android
service to perform periodical update checks.
