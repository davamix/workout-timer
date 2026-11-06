import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "io.github.davamix.workouttimer"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.davamix.workouttimer"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // The keystore and its passwords stay outside the repo, in the user-level gradle.properties (plan §3). Without
        // all four properties, as on CI, the release build is unsigned.
        val keystoreFile = providers.gradleProperty("workoutTimerKeystoreFile").orNull
        val keystorePassword = providers.gradleProperty("workoutTimerKeystorePassword").orNull
        val alias = providers.gradleProperty("workoutTimerKeyAlias").orNull
        val aliasPassword = providers.gradleProperty("workoutTimerKeyPassword").orNull
        if (keystoreFile != null && keystorePassword != null && alias != null && aliasPassword != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = alias
                keyPassword = aliasPassword
            }
        }
    }

    buildTypes {
        debug {
            // Lets debug and release builds live side by side on the tablet (P1-D2).
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }

    testOptions {
        // :app unit tests use JUnit Jupiter, like :core (P3).
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// NF-4: the app must never request network access. Fails `check` if any merged manifest asks for INTERNET,
// for example because a library added it.
abstract class CheckNoInternetPermission : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @TaskAction
    fun check() {
        val manifest = mergedManifest.get().asFile.readText()
        val internet = Regex("""<uses-permission[^>]*android:name="android\.permission\.INTERNET"""")
        if (internet.containsMatchIn(manifest)) {
            throw GradleException(
                "The merged manifest requests android.permission.INTERNET, which NF-4 forbids. " +
                    "Find the dependency that adds it.",
            )
        }
    }
}

androidComponents {
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val checkTask =
            tasks.register<CheckNoInternetPermission>("check${variantName}NoInternetPermission") {
                group = "verification"
                description = "Fails if the merged ${variant.name} manifest requests INTERNET (NF-4)."
                mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            }
        tasks.named("check") { dependsOn(checkTask) }
    }
}
