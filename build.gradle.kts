// Top-level build file. AGP 9 compiles Kotlin itself; the kotlin-android entry only pins the KGP version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
