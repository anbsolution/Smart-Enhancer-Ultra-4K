plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.anb.smartenhancer"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.anb.smartenhancer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
