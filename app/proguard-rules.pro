# The app uses no reflection, no dynamic class loading and no serialization
# framework, so the defaults in proguard-android-optimize.txt are sufficient.
# R8 keeps Compose, OkHttp and androidx via their own consumer rules.

# OkHttp references these optionally.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink, which backs EncryptedSharedPreferences, is compiled against these
# errorprone annotations. They are annotation-only and never present at runtime,
# so there is nothing to keep — this is the rule set R8 itself generated in
# app/build/outputs/mapping/release/missing_rules.txt.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
