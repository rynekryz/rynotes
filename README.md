<div align="center">
<img src="icon.png" width="160" height="160" style="display: block; margin: 0 auto"/>
<h1>RyNotes</h1>
<p>A modern, lightweight, and secure notes application for Android. Built with **Jetpack Compose** and **Material 3 Expressive**</p>
<p>Offers a beautiful, fluid user experience while prioritizing your privacy and organization.</p>
</div>

> **Status:** Early alpha. Expect bugs and breaking changes between versions. See [Releases](https://github.com/rynekryz/rynotes/releases) for the latest APK.

---

## Features

### Beautiful Design
- **Material 3 Expressive:** Leveraging the latest Material 3 components for a bold and modern look.
- **Dynamic Color (Material You):** Themes that adapt to your wallpaper (Android 12+).
- **Dark Mode & OLED Support:** Support for standard dark mode and a "Pure Black" mode for OLED screens.
- **Note Colors:** Personalize notes with 6 curated accent colors.

### Privacy & Security
- **App Lock:** Secure the entire app using a PIN, Pattern, Password, or Biometric authentication.
- **The Vault:** A dedicated private space for your most sensitive notes, protected by its own secret.
- **Biometric Integration:** Seamlessly unlock your app or vault using fingerprint or face unlock.

### Organization & Workflow
- **Folders:** Categorize your notes into custom folders for better management.
- **Tags:** Add searchable tags to your notes.
- **Pin & Favorite:** Keep important notes at the top or mark them for easy access.
- **Archive:** Hide notes from your main grid without deleting them.
- **Trash Bin:** Deleted notes are safely kept for 30 days before being permanently removed.
- **Search:** Quickly find notes with real-time title and content search.

### Advanced Tools
- **Markdown Support:** Write notes with lightweight Markdown-style formatting (Bold, Italic, Underline, Bullets).
- **PDF Viewer:** View PDF documents directly within the app.
- **Backup & Restore:** Export your data as a secure ZIP/JSON backup and restore it anytime.
- **Haptic Feedback:** Tactile responses for a more immersive experience.
- **Customization:** Adjust font scales and font types to your preference.

---

## Technical Stack

- **UI:** Jetpack Compose with Material 3 Expressive
- **Architecture:** MVVM (Model-View-ViewModel)
- **Data Persistence:** Jetpack DataStore (Preferences)
- **Language:** 100% Kotlin
- **Build System:** Gradle (Kotlin DSL)

---

## Download

Grab the latest APK from the [Releases page](https://github.com/rynekryz/rynotes/releases). Pick the build matching your device:

| Variant | Use case |
|---|---|
| `arm64` | Most modern phones (recommended) |
| `armv7` | Older 32-bit devices |
| `x86_64` | Emulators / x86 devices |
| `universal` | Works on any device, larger file size |

> Alpha/Beta builds are **debug builds**, not signed for production release yet.

---

## Getting Started

### Prerequisites
- Android Studio (latest stable)
- JDK 21
- Android SDK 37 (compile target)
- Minimum SDK: Android 12 (API 31)

### Build
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle and run the `:app` module.

```bash
# Build debug APK (all ABIs + universal)
./gradlew assembleDebug
```

---

## Project Structure

```
RyNotes/
├── app/
│   ├── src/main/java/com/rynekryz/rynotes/
│   │   ├── MainActivity.kt       # Navigation & Entry Point
│   │   ├── HomeScreen.kt         # Main Grid & Search
│   │   ├── NoteEditorScreen.kt   # Rich Note Editing
│   │   ├── VaultScreen.kt        # Secure Notes Container
│   │   ├── SettingsScreen.kt     # App Preferences & Backup
│   │   ├── NoteViewModel.kt      # Core Logic & Persistence
│   │   ├── Note.kt               # Data Models
│   │   └── ...                   # Biometrics, Haptics, Theme, etc.
│   └── src/main/AndroidManifest.xml
└── ...
```

---

## License

Distributed under the GNU General Public License v3.0 (GPLv3). See [LICENSE](LICENSE) for more information.

---

## Contributing

Contributions are welcome! Feel free to open [issues](https://github.com/rynekryz/rynotes/issues) or submit pull requests.

<div align="center">
<strong>Made with ♥︎ by Ryne</strong>
</div>
<div align="center">
  <a href="DISCORD_INVITE"><img src="https://img.shields.io/badge/Discord-5A5A5A?style=for-the-badge&logo=discord&logoColor=white"/></a>
  <a href="https://tiktok.com/@rynekryz"><img src="https://img.shields.io/badge/TikTok-5A5A5A?style=for-the-badge&logo=tiktok&logoColor=white"/></a>
</div>