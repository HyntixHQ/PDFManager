# Changelog

All notable changes to PDF Manager will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
 
## [1.3.0] - 2026-01-26

### Changed
- **Relicensed to AGPL-3.0**: Switched from MIT to GNU Affero General Public License v3.0 for better protection.
- **Library Updates**: Updated `HyntixPdfViewer` and `KotlinPdfium` to version `1.0.3` (AGPL-3.0).

## [1.2.0] - 2026-01-24
 
### Fixed
- **API Synchronization**: Updated to support latest `HyntixPdfViewer` API changes.
- **Search Fixes**: Improved search results handling and stability.
 
### Changed
- Switched dependency to remote `HyntixPdfViewer:1.0.2`.
- Bumped application version to v1.2.0.

## [1.1.0] - 2026-01-21

### Fixed
- **Improved PDF Gestures**: Upgraded `HyntixPdfViewer` to v1.0.1 to bring carousel-style swipe gestures and improved zoom handling.
- **Carousel Swipe**: Any light swipe now transitions to the next/previous page smoothly in page-by-page mode.
- **Zoom Fix**: Pinch/Double-tap zoom no longer triggers accidental page changes.
- **Horizontal Display**: Pages are now correctly centered and displayed one at a time in horizontal mode.

### Changed
- Switched dependency to remote JitPack version of `HyntixPdfViewer:1.0.1`.
- Bumped application version to v1.1 (versionCode 2).

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

