# GeoTask 📍

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%20Design-3-6750A4)](https://m3.material.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An ultra-minimalist, battery-conscious location-based reminder and proximity task manager for Android. GeoTask monitors your physical geographic location and triggers rich, actionable heads-up notifications whenever you enter your designated places.

---

## ✨ Features

- **📍 Geographic Proximity Alerts:** Automatically alerts you when you arrive at specific places (Supermarket, Office, Pharmacy, Home, Gym, etc.).
- **🎯 Configurable Geofence Radius:** Select preset trigger radii (`50m`, `100m`, `200m`, `500m`, `1000m`) or manually enter any custom radius in meters.
- **✍️ Flexible Location Input:** Pick from instant 1-tap presets or manually add custom place names and GPS coordinates.
- **⚡ Instant Notification Tester:** Test proximity alerts on an emulator or physical device with 1 tap using the built-in simulator trigger.
- **🔔 Actionable Notifications:** Complete tasks or snooze alerts directly from the Android notification shade.
- **💾 100% Offline & Private:** Powered by a local **Room Database**; all your coordinates and reminders remain strictly on your device.
- **🌿 Clean & Clutter-Free:** Designed with Material Design 3 and a minimalist UX—zero unnecessary menus, just fast and effective task tracking.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin (100%)
- **UI Toolkit:** Jetpack Compose with Material Design 3 (M3)
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Persistence:** Room Database (with KSP)
- **Location Services:** Google Play Services Fused Location Provider + Foreground Service (`FOREGROUND_SERVICE_LOCATION`)
- **Concurrency:** Kotlin Coroutines & Reactive `StateFlow`
- **Build System:** Gradle (Kotlin DSL `.gradle.kts`) with Version Catalogs (`libs.versions.toml`)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2+) or newer
- **JDK:** OpenJDK 17 or 21
- **Android Device / Emulator:** Android 7.0 (API 24) or higher

### Clone & Build

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/geotask.git
   cd geotask
   ```

2. Restore the debug keystore (if building from a fresh clone):
   ```bash
   base64 -d debug.keystore.base64 > debug.keystore
   ```

3. Build the debug APK using the Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔄 CI/CD & GitHub Actions

You can automatically build installable debug APKs on GitHub Actions by adding the following workflow file at `.github/workflows/build-apk.yml`:

```yaml
name: Build Debug APK

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    name: Build Android Debug APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Prepare Keystore & Environment
        run: |
          if [ -f debug.keystore.base64 ]; then
            base64 -d debug.keystore.base64 > debug.keystore
          elif [ ! -f debug.keystore ]; then
            keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
          fi
          touch .env

      - name: Build Debug APK
        run: |
          gradle :app:assembleDebug --stacktrace

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: GeoTask-Debug-APK
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 14
```

---

## 📄 License & Attribution

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

### Attribution Requirement
If you clone, fork, or build upon this project for your own application or portfolio, **attribution to the original author is required**. Please include a link to this repository and preserve the following copyright notice in your documentation or credits:

```text
Original Project: GeoTask by Nasif (nasifwrites@gmail.com)
Repository: https://github.com/nasifwrites/geotask (or original repository URL)
```

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](../../issues).

Made with ❤️ by [Nasif](mailto:nasifwrites@gmail.com)
