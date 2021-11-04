# when the git tag was created by the web-frontend, you have to do:
git fetch --tags

# release new version
 - are you on the master branch?
 - optional: `./gradlew wrapper --gradle-version=6.8.2` https://gradle.org/releases/
 - optional: `./gradlew clean adviceRelease`
 - fix/add unit tests
 - check translations
 - edit build.gradle to increase `versionCode` and `versionName`
 - add changelog to CHANGELOG.md
 - add supporter to CHANGELOG.md
 - copy entry from CHANGELOG.md to fastlane/metadata/android/en-US/changelogs/VERSION_CODE.txt
 - commit changes with references to the issues/pull requests
   - `Tobiwan/ffupdater#XXX` for issues/pull requests from notabug.org
   - `Tobi823/ffupdater#XXX` for issues/pull requests from Github
   - `Tobiwan/ffupdater_gitlab#XXX` for issues/merge requests from Gitlab
 - create tag for release
   ````bash
   TAG="XXXXXX"
   git tag "$TAG"
   git pushall
   git push origin "$TAG"
   git push github "$TAG"
   git push gitlab "$TAG"
   git push gitea "$TAG"
   
   ````
 - push tag to notabug.org, Github and Gitlab
   ````bash
   git config --global alias.pushall '!git remote | xargs -L1 git push --all'
   git pushall
   ````
 - build signed APK file
 - upload signed APK file to GitHub and GitLab
 - start repomaker with Docker
 - upload signed APK to repomaker
 - test release with "Foxy Droid"

# delete all remote branches except master
````
REMOTE_NAME="XXXXX"
git branch -r | grep "$REMOTE_NAME/" | grep -v 'master$' | grep -v HEAD| cut -d/ -f2 | while read line; do git push "$REMOTE_NAME" :$line; done;
````

You can either wait for the official F-Droid release, install the APK file from GitHub/GitLab or try my personal F-Droid repository (see https://github.com/Tobi823/ffupdater#f-droid-repository).