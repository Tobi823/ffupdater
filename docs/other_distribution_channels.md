# Other distribution channels

The main distribution method of FFUpdater remains F-Droid - this will not change. But you can use the APK
files or the F-Droid repository to quickly install fixed versions.

You need to uninstall FFUpdater every time you switch between F-Droid version and my version. F-Droid will not
show you new updates if you still uses the version from GitHub/GitLab.

My versions will be signed with this [certificate](dev/signatures/apk_signature.txt).

The official F-Droid Android client sometimes has problems accessing the APK file from my F-Droid repository.
But other clients like "Foxy Droid" work fine.

## APK files on GitHub

The APK files are available on [GitHub](https://github.com/Tobi823/ffupdater/releases/latest) and
[GitLab](https://gitlab.com/Tobiwan/ffupdater_gitlab/-/releases/latest).

## F-Droid repository

### Method 1

Repository address with fingerprint: `https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo?fingerprint=6E4E6A597D289CB2D4D4F0E4B792E14CCE070BDA6C47AF4918B342FA51F2DC89`

Copy the address and paste it into F-Droid.

[![Video](fdroid_repo_method1.mp4)](fdroid_repo_method1.mp4)


### Method 2

Scan the QR-Code and open the link with F-Droid.

[![Add](https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo/assets/qrcode.png)](https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo/assets/qrcode.png)

### Method 3

(This method might be broken). Follow the link and click on the link.

[Add the repository to F-Droid](https://tobi823.github.io/ffupdaterrepo.html)

### Debug

[How to Add a Repo to F-Droid](https://f-droid.org/en/tutorials/add-repo/)

It seems that this F-Droid repository is sometimes a little buggy. If F-Droid fails to download FFUpdater, try
to install the version from the official F-Droid repository first.

On the app page under the item "Versions" you can see from which repository (my *FFUpdater*
repository or the official *F-Droid* repository) the app version was installed