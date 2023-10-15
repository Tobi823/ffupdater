# when the git tag was created by the web-frontend, you have to do:

git fetch --tags

# release new version

- on master branch?
- optional:
  - change Gradle version in gradle/wrapper/gradle-wrapper.properties
  - `gradle wrapper`
  - `./gradlew clean adviceRelease`
  - double check that Android Studio wont reformat gradlew
- fix and add unit tests
- ,ff_weblate
- build.gradle: versionCode + versionName
- commit changes:
  - Release version xx (xx)
  - Weblate contributors
  - `Tobiwan/ffupdater#XXX` for issues/pull requests from notabug.org
  - `Tobi823/ffupdater#XXX` for issues/pull requests from Github
  - `Tobiwan/ffupdater_gitlab#XXX` for issues/merge requests from Gitlab
  - (fastlane changelog will be managed by ,ff_release)
- `,ff_release`
- in Firefox upload APK in repomaker
- create Gitlab merge request (see https://gitlab.com/fdroid/fdroiddata/-/merge_requests/13713)

```
Changelogs:
* https://github.com/Tobi823/ffupdater/blob/master/CHANGELOG.md
* https://github.com/Tobi823/ffupdater/compare/75.5.1...75.5.2
```

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