# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Jetpack Compose (Rely on library rules)

# Room (Rely on library rules)
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static <methods>;
}



# Models used in serialization or reflection
-keep class com.hyntix.android.pdfmanager.data.model.** { *; }

# Keep generic ViewModel names
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep NativeScanner and its inner classes for JNI calls from Rust
-keep class com.hyntix.android.pdfmanager.native.NativeScanner { *; }
-keep class com.hyntix.android.pdfmanager.native.NativeScanner$* { *; }

# Keep BitmapOps for JNI calls from C++
-keep class com.hyntix.android.pdfmanager.native.BitmapOps { *; }
