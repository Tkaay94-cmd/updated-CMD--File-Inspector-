plugins {
    id("com.android.application") version "8.2.2" apply false
    kotlin("android") version "1.9.20" apply false
    kotlin("kapt") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.47" apply false
}
val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val keyAliasValue = System.getenv("ANDROID_KEY_ALIAS")
val keyPasswordValue = System.getenv("ANDROID_KEY_PASSWORD")

val hasReleaseSigning =
    !keystorePath.isNullOrBlank() &&
    !keystorePassword.isNullOrBlank() &&
    !keyAliasValue.isNullOrBlank() &&
    !keyPasswordValue.isNullOrBlank()

android {
    // Existing configuration...

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

For production CI, it is better to fail clearly if signing secrets are absent. Add this near the top of the file if release signing must always be enforced in CI:
if (System.getenv("CI") == "true" && !hasReleaseSigning) {
    throw GradleException(
        "Production signing is required in CI, but signing environment variables are missing."
    )
}
