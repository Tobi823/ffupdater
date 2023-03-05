# Checklist for adding an end-of-life (eol) apps:

- add/set eolReason
- change displayCategory to DisplayCategory.EOL
- update README.md
- update fastlane/metadata/android/en-US/{full_description.txt,short_description.txt}
  - update description in repomaker
- remove old unit tests + test resources