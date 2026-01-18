cmake_minimum_required(VERSION 3.22.1)
project("bitmap_ops")

# Create shared library
add_library(bitmap_ops SHARED
    bitmap_ops.cpp
)

# Link required Android libraries
target_link_libraries(bitmap_ops
    android
    jnigraphics
    log
)

# Enable NEON for ARM64 (enabled by default on arm64-v8a)
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    target_compile_options(bitmap_ops PRIVATE -O3)
endif()

# Compiler optimizations
target_compile_options(bitmap_ops PRIVATE
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
)

# Linker optimizations
target_link_options(bitmap_ops PRIVATE
    -Wl,--gc-sections
)
