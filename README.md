<div align="center">
<img src="icon.png" width="160" height="160" style="display: block; margin: 0 auto"/>
<h1>RyNotes</h1>
<p>A private notes app for Android, built with Jetpack Compose and Material 3 Expressive.</p>
<p>Organize notes with folders, tags, and a vault, all stored on your device.</p>
</div>
<p align="center">
  <a href="https://github.com/rynekryz/rynotes/stargazers"><img src="assets/badges/stars.svg" alt="GitHub stars" height="30"/></a>
  <a href="https://github.com/rynekryz/rynotes/tags"><img src="assets/badges/release.svg" alt="Latest release" height="30"/></a>
  <a href="LICENSE"><img src="assets/badges/licencegplv3.svg" alt="Licence GPLv3"/></a>
  <img src="assets/badges/android.svg" alt="Android"/>
  <img src="assets/badges/kotlin.svg" alt="Kotlin"/>
</p>

<p align="center">
  <a href="https://github.com/rynekryz/rynotes/releases"><img src="assets/badges/getgithub.svg" alt="Get it on GitHub" height="56"/></a>
</p>

---

## Screenshots

<div align="center">
<img src="/Screenshots/101660.png" width="30%" />
<img src="/Screenshots/101662.png" width="30%" />
<img src="/Screenshots/101664.png" width="30%" />
<img src="/Screenshots/101666.png" width="30%" />
<img src="/Screenshots/101668.png" width="30%" />
<img src="/Screenshots/101670.png" width="30%" />
</div>

---

## Features

- Material 3 Expressive, with dynamic color from your wallpaper (Android 12+)
- Dark mode and a pure black OLED mode
- Six curated note colors, so things feel like yours
- App lock with PIN, pattern, password, or biometrics
- A Vault with its own secret for the sensitive stuff (your diary is safe here)
- Folders, tags, pin, favorite, and archive
- Trash that holds deleted notes for 30 days, in case you change your mind (you will)
- Real-time search across titles and content
- Light Markdown (bold, italic, underline, bullets)
- Built-in PDF viewer
- Backup and restore as a ZIP/JSON file
- etc.

---

## Technical Stack

- **UI:** Jetpack Compose with Material 3 Expressive
- **Architecture:** MVVM
- **Persistence:** DataStore Preferences
- **Security:** BiometricPrompt with an AES vault
- **Language:** Kotlin
- **Build:** Gradle (Kotlin DSL)

---

## Building

You'll need Android Studio (latest stable), JDK 21, and SDK 37. Minimum is Android 12 (API 31).

1. Clone the repo and open it in Android Studio.
2. Sync Gradle and run the `:app` module.

```bash
./gradlew assembleDebug
```

`arm64` fits most phones, `armv7` is for older 32-bit ones, `x86_64` is for emulators, and `universal` works anywhere if you're not sure.

---

## Project Structure

```
RyNotes/
└── app/src/main/java/com/rynekryz/rynotes/
    ├── MainActivity.kt
    ├── HomeScreen.kt
    ├── NoteEditorScreen.kt
    ├── VaultScreen.kt
    ├── SettingsScreen.kt
    ├── NoteViewModel.kt
    ├── Note.kt
    └── ...
```

---

## License

Licensed under GPL-3.0. See [LICENSE](LICENSE).

<div align="center">
<strong>Made with ♥︎ by Ryne</strong>
</div>
<div align="center">
  <a href="https://discord.gg/YKhWXXhKmC"><img src="assets/badges/discord.svg" alt="Discord"/></a>
  <a href="https://tiktok.com/@rynekryz"><img src="assets/badges/tiktok.svg" alt="TikTok"/></a>
</div>
