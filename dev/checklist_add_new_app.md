# Checklist for adding new apps:
- add new class in de.marmaro.krt.ffupdater.app.impl
- add entry in de.marmaro.krt.ffupdater.app.App
- add entry in the arrays disabledAppEntries + disabledAppEntryValues in src/main/res/values/arrays.xml
- add entry in AndroidManifest.xml <queries>
- update README.md
- update (full/short)_description.txt
- download icon
- update content_main.xml
- update/add unit tests