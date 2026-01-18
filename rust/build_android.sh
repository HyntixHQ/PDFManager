#!/bin/bash
# Build script for Rust PDF scanner - ARM64 only
# Uses NDK version 29.0.14206865

set -e

# Configuration
NDK_VERSION="29.0.14206865"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ANDROID_NDK_HOME="${ANDROID_HOME}/ndk/${NDK_VERSION}"
TARGET_ABI="arm64-v8a"
RUST_TARGET="aarch64-linux-android"

# Verify NDK exists
if [ ! -d "$ANDROID_NDK_HOME" ]; then
    echo "Error: NDK not found at $ANDROID_NDK_HOME"
    echo "Please install NDK version $NDK_VERSION via Android Studio SDK Manager"
    exit 1
fi

# Install cargo-ndk if not present
if ! command -v cargo-ndk &> /dev/null; then
    echo "Installing cargo-ndk..."
    cargo install cargo-ndk
fi

# Add Android target
rustup target add $RUST_TARGET

# Set NDK path for cargo-ndk
export ANDROID_NDK_HOME

cd "$(dirname "$0")/pdf_scanner"

echo "Building Rust library for $TARGET_ABI..."
echo "Using NDK: $ANDROID_NDK_HOME"

# Build for ARM64 only
cargo ndk -t $TARGET_ABI \
    -o ../../app/src/main/jniLibs \
    build --release

echo ""
echo "✅ Build complete!"
echo "Library copied to app/src/main/jniLibs/$TARGET_ABI/"
ls -la ../../app/src/main/jniLibs/$TARGET_ABI/*.so 2>/dev/null || echo "Note: Library files will appear after first successful build"
