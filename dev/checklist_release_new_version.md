# when the git tag was created by the web-frontend, you have to do:
git fetch --tags

# release new version
 - `./gradlew wrapper --gradle-version=6.8.2` https://gradle.org/releases/
 - `gradlew clean adviceRelease`
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
 - push tag to notabug.org, Github and Gitlab

# push to git
https://stackoverflow.com/a/18674313
````bash
git config --global alias.pushall '!git remote | xargs -L1 git push --all'
git pushall
````

# push tag to git
````bash
TAG="XXXXXX"
git tag "$TAG"
git pushall
git push origin "$TAG"
git push github "$TAG"
git push gitlab "$TAG"
git push gitea "$TAG"
````

