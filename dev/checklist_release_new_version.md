# when the git tag was created by the web-frontend, you have to do:

git fetch --tags

# release new version

- on master branch?
- optional:
  - change Gradle version in gradle/wrapper/gradle-wrapper.properties
  - `gradle wrapper`
  - `./gradlew clean adviceRelease`
- fix and add unit tests
- ,ff_weblate
- build.gradle: versionCode + versionName
- sync gradle
- CHANGELOG.md: Weblate (https://hosted.weblate.org/projects/ffupdater/#reports) + changelog + supporter
- copy changelog to `fastlane/metadata/android/en-US/changelogs/VERSION_CODE.txt`
- commit changes:
  - Release version xx (xx)
  - `Tobiwan/ffupdater#XXX` for issues/pull requests from notabug.org
  - `Tobi823/ffupdater#XXX` for issues/pull requests from Github
  - `Tobiwan/ffupdater_gitlab#XXX` for issues/merge requests from Gitlab
- build signed APK file
- copy message of the release commit
- `,ff_release "???"` to tag, push, create releases on GitHub/GitLab, start repomaker+Docker
- upload APK in repomaker
- upload signature, create PR, update metadata in PR -
  see https://gitlab.com/fdroid/fdroiddata/-/merge_requests/13713.
  - `cd /home/hacker/Documents/Code/kein_backup/gitlab.com/Tobiwan/fdroiddata/metadata/de.marmaro.krt.ffupdater/signatures/`
  - `mkdir $versionCode`
  - `cp /home/hacker/Documents/Code/github.com/Tobi823/ffupdater/ffupdater/release/ffupdater-release.apk .`
  - `apksigcopier extract --v1-only=auto ffupdater-release.apk .`
  - `rm ffupdater-release.apk`

```
Changelogs:
* https://github.com/Tobi823/ffupdater/blob/master/CHANGELOG.md
* https://github.com/Tobi823/ffupdater/compare/75.5.1...75.5.2
```

- upload signed APK to repomaker
- test release with "Foxy Droid"
- mark older changelogs in Weblate as read-only
- use different branch when working on fdroiddata

# delete all remote branches except master

````
REMOTE_NAME="XXXXX"
git branch -r | grep "$REMOTE_NAME/" | grep -v 'master$' | grep -v HEAD| cut -d/ -f2 | while read line; do git push "$REMOTE_NAME" :$line; done;
````

You can either wait for the official F-Droid release, install the APK file from GitHub/GitLab or try my
personal F-Droid repository (see https://github.com/Tobi823/ffupdater#f-droid-repository).