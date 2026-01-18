# Development Guide - PDF Manager

Welcome to the development guide for PDF Manager.

## Prerequisites

- **Android Studio**: Latest stable version (Ladybug or newer recommended)
- **JDK**: Version 21
- **Android SDK**: API 36 (compile SDK)
- **NDK**: Version 29.0.14206865
- **Rust**: For PDF scanner and encryption modules

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd PDFManager
```

### 2. Configure Keystore (for Release Builds)

Create `keystore.properties` in the project root:

```properties
storeFile=/absolute/path/to/your-keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

> ⚠️ **Never commit this file to version control!**

### 3. Sync and Build

Open the project in Android Studio and let Gradle sync. Then:

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Project Modules

| Module | Description |
|--------|-------------|
| `app` | Main application with UI and business logic |
| `HyntixPdfViewer` | PDF viewer component (Referenced via JitPack by default) |
| `KotlinPdfium` | Core PDF engine (Referenced via JitPack by default) |
| `rust/pdf_scanner` | Rust-based PDF scanning utilities |


## Architecture

The app follows a simplified MVVM architecture:

```
┌─────────────────────────────────────────────────┐
│                      UI Layer                    │
│   (Composables, Screens, ViewModels)            │
├─────────────────────────────────────────────────┤
│                   Domain Layer                   │
│   (Repositories, Use Cases)                     │
├─────────────────────────────────────────────────┤
│                    Data Layer                    │
│   (Room Database, DataStore, File System)       │
└─────────────────────────────────────────────────┘
```

## Key Technologies

### Navigation

The app uses **Navigation3** (`androidx.navigation3`), the latest navigation library for Compose.

### State Management

- **ViewModels** with `StateFlow` for UI state
- **DataStore** for preferences
- **Room** for structured data persistence

### PDF Rendering

PDF rendering is handled by the custom `HyntixPdfViewer` library which wraps PDFium:

```kotlin
// Example usage
PdfViewerScreen(
    uri = pdfUri,
    scrollMode = ScrollMode.VERTICAL,
    onBack = { /* handle back */ }
)
```

## Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Write self-documenting code with comments for complex logic
- Keep functions small and focused

## Building for Release

### APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### App Bundle (Play Store)

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Troubleshooting

### NDK Not Found

Ensure NDK is installed via SDK Manager:
- Open Android Studio → Settings → SDK Manager
- Select SDK Tools tab
- Check "NDK (Side by side)" and install version 29.0.14206865

### Keystore Issues

If you see signing errors:
1. Verify the path in `keystore.properties` is absolute
2. Ensure the file exists and is readable
3. Verify passwords are correct

### Gradle Sync Failures

Try these steps:
1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.gradle` and `build` directories
3. Re-sync the project

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## Useful Commands

```bash
# Clean build
./gradlew clean

# Clean release build and install
./gradlew clean assembleRelease && adb install -r app/build/outputs/apk/release/app-release.apk

# List all tasks
./gradlew tasks

# Generate lint report
./gradlew lint
```
