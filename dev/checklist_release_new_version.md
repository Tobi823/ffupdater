# when the git tag was created by the web-frontend, you have to do:
git fetch --tags

# release new version
 - `gradlew clean adviceRelease`
 - fix/add unit tests
 - check translations
 - add changelog to CHANGELOG.md
 - add supporter to CHANGELOG.md
 - copy entry from CHANGELOG.md to fastlane/metadata/android/en-US/changelogs/VERSION_CODE.txt
 - edit gradle.build to increase version code and version name
 - commit changes
 - create tag for release
 - push tag to notabug.org, Github and Gitlab

# push tag to git
## Windows
````powershell
SET TAGVALUE=68.4.0
git tag %TAGVALUE%
git push origin %TAGVALUE%
git push github %TAGVALUE%
git push gitlab %TAGVALUE%
````

## Linux
````bash
read -i "TAG: " TAG && git tag $TAG && git push origin $TAG && git push github $TAG && git push gitlab $TAG
````