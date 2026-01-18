# Changelog

All notable changes to PDF Manager will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-18

### Added
- PDF rendering via custom HyntixPdfViewer library powered by PDFium
- Home screen with All PDFs, Recent, and Favorites tabs
- Virtual Folders for organizing PDFs without moving files
- File scanning with pull-to-refresh
- Duplicate Finder to detect and manage duplicate PDFs
- Search functionality for finding PDFs
- PDF viewer with pinch-to-zoom and pan
- Text search within PDF documents
- Table of Contents navigation
- Text selection and copy support
- Reflow mode for readable text display
- Password-protected PDF support
- Share PDF functionality
- Multi-select mode for bulk operations
- Settings: Theme, Grayscale mode, Keep screen awake
- Privacy Policy and Terms of Service
- Storage permission handling for Android 13+

### Changed
- Migrated to remote JitPack dependencies for `HyntixPdfViewer` and `KotlinPdfium`.
- Standardized build system with Gradle 9.1 and JDK 21.
- Optimized release build with full R8 shrinking and minification.

