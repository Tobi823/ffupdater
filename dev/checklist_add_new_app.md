# Checklist for adding new apps:
- download icon (ic_launcher)
- add new class in de.marmaro.krt.ffupdater.app.impl
- add entry in de.marmaro.krt.ffupdater.app.MaintainedApp
- add entry in the arrays background__update_check__excluded_apps__entries +
  background__update_check__excluded_apps__values in src/main/res/values/arrays.xml
- add entry in AndroidManifest.xml <queries>
- update README.md
- update fastlane/metadata/android/en-US/{full_description.txt,short_description.txt}
  - update description in repomaker
- update/add unit tests
- resolved all TODOs?
- add it to CHANGELOG.md?

Extract certificate hash with: `keytool -printcert -jarfile *.apk`