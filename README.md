# RyNotes

**RyNotes** is a modern, lightweight, and secure notes application for Android. Built with **Jetpack Compose** and **Material 3 Expressive**, it offers a beautiful, fluid user experience while prioritizing your privacy and organization.

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

- **UI:** Jetpack Compose (1.4.0+ Material 3 Expressive)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Data Persistence:** Jetpack DataStore (Preferences)
- **Language:** 100% Kotlin
- **Build System:** Gradle (Kotlin DSL)

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK 35 (Target)
- Minimum SDK: Android 5.0 (API 21)

### Build
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle and run the `:app` module.

```bash
# Build debug APK
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

Distributed under the GNU General Public License v3.0 (GPLv3). See `LICENSE` for more information.

---

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests.
