# Security measures

- The signature fingerprint of every downloaded APK file is validated against an internal allowlist. This
  prevents the installation of malicious apps that do not originate from the original developers.
- Only HTTPS connections are used because unencrypted HTTP traffic can be manipulated.
- Only system certificate authorities are trusted. But this can be disabled in the settings to allow other
  apps to inspect the application's network traffic.
- Prevent command injection in the RootInstaller.kt by validating and sanitizing commands.
- ~~Git commits will be signed with the GPG key
  CE72BFF6A293A85762D4901E426C5FB1C7840C5F [public key](ffupdater_gpg_public.key)~~
- Git commits will be signed with the ssh-ed25519 key:
  AAAAC3NzaC1lZDI1NTE5AAAAIJE17LRw9gdAka03KYwdFj88b3sDEODRBlIY1smsvOMx [public key](ffupdater_git_signing_key_ed25519.pub)
- APK will be signed with the key [apk_signature](../dev/signatures/apk_signature.txt)