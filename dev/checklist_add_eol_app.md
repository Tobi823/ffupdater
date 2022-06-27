# Checklist for adding an end-of-life (eol) apps:

- move old class from de.marmaro.krt.ffupdater.app.impl to de.marmaro.krt.ffupdater.app.eol
- change base class to EolBaseApp
- add entry in de.marmaro.krt.ffupdater.eol.EolApp
- remove entry from arrays background__update_check__excluded_apps__entries +
  background__update_check__excluded_apps__values in src/main/res/values/arrays.xml
- add comment to the entry in AndroidManifest.xml <queries>
- update README.md
- update fastlane/metadata/android/en-US/{full_description.txt,short_description.txt}
    - update description in repomaker
- remove old unit tests + test resources