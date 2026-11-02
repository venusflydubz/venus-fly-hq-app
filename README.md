# Venus Fly HQ (Android)

The private dashboard at https://venusflydubz.com/dashboard as an Android app for the
Galaxy S26 Ultra and Galaxy Tab S10 FE. The app is a full-screen shell around the live
dashboard, so new dashboard features show up without updating the app.

- Login, every tab, Ask HQ, the documents vault, uploads (with camera) and downloads
  (saved to Downloads/Venus Fly HQ).
- Long-press the icon for Inbox, Calendar, Income and Ask HQ shortcuts.
- Links outside the dashboard (Instagram, SoundCloud, email) open in their own apps.

## Getting the APK
Every push to `main` builds and signs the APK with GitHub Actions and attaches it to a
new release. Open **Releases** on your phone, tap the newest `.apk`, and allow installs
from your browser if Android asks.

Release builds are signed with the permanent key from the repository secrets `VF_KEYSTORE_B64`, `VF_KEYSTORE_PASSWORD`,
`VF_KEY_ALIAS` and `VF_KEY_PASSWORD`. Keep the same key, or Android will refuse to
install updates over the old app.
