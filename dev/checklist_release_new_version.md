# when the git tag was created by the web-frontend, you have to do:

git fetch --tags

# release new version

- are you on the master branch?
- optional:
  - `./gradlew wrapper --gradle-version=6.8.2` https://gradle.org/releases/
  - `./gradlew clean adviceRelease`
- fix and add unit tests
- `,ff_weblate`
- edit build.gradle to increase `versionCode` and `versionName`
- add changelog and supporter to CHANGELOG.md
- add Weblate contribution (don't forget commits from Weblate)
  - apply suggestions from Weblate
  - use button on https://hosted.weblate.org/projects/ffupdater/#reports
- copy changelog from CHANGELOG.md to `fastlane/metadata/android/en-US/changelogs/VERSION_CODE.txt`
- commit changes with references to the issues/pull requests
  - `Tobiwan/ffupdater#XXX` for issues/pull requests from notabug.org
  - `Tobi823/ffupdater#XXX` for issues/pull requests from Github
  - `Tobiwan/ffupdater_gitlab#XXX` for issues/merge requests from Gitlab
- build signed APK file
- copy message of the release commit
- `,ff_release "???"` to tag, push, create releases on GitHub/GitLab, start repomaker+Docker

```
Changelogs:
* https://github.com/Tobi823/ffupdater/blob/master/CHANGELOG.md
* https://github.com/Tobi823/ffupdater/compare/75.5.1...75.5.2
```

- upload signed APK to repomaker
- test release with "Foxy Droid"
- mark older changelogs in Weblate as read-only

# delete all remote branches except master

````
REMOTE_NAME="XXXXX"
git branch -r | grep "$REMOTE_NAME/" | grep -v 'master$' | grep -v HEAD| cut -d/ -f2 | while read line; do git push "$REMOTE_NAME" :$line; done;
````

You can either wait for the official F-Droid release, install the APK file from GitHub/GitLab or try my
personal F-Droid repository (see https://github.com/Tobi823/ffupdater#f-droid-repository).