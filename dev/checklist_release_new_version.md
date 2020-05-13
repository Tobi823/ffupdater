# when the git tag was created by the web-frontend, you have to do:
git fetch --tags

# release new version
 - fix/add unit tests
 - check translations
 - add changelog to NEWS
 - add supporter to NEWS
 - edit gradle.build to increase version code and version name
 - commit changes
 - create tag for release
 - push tag to notabug.org, Github and Gitlab


# push tag to git
## Windows
````powershell
SET TAG=67.4
git tag %TAG%
git push origin %TAG%
git push github %TAG%
git push gitlab %TAG%
````

## Linux
````bash
TAG=67.1
git tag $TAG
git push origin $TAG
git push github $TAG
git push gitlab $TAG
````