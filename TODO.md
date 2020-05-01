  - test app
  - write unit tests
  - update translations
  - add log messages for background tasks (for better understanding)
  - for newer android version check for write permission!
  if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PERMISSION_DENIED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)) {
          // TODO show reason why we need the WRITE_EXTERNAL_STORAGE permission
      } else {
          ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE);
      }
      return;
  }
  
  // https://www.baeldung.com/java-download-file