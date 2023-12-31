plugins {
    id("com.android.application")
}

android {
    // Gets the latest git commit hash for autoversioning
    val gitCommitHash = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
    // Gets the total number of commits for autoversioning
    val gitCommitCount = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.get().trim().toInt()
    // Gets the latest git tag for autoversioning
    val gitLatestTag = providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
    }.standardOutput.asText.get().trim()

    namespace = "lightjockey.mqttdroid"
    compileSdk = 34

    defaultConfig {
        applicationId = "lightjockey.mqttdroid"
        minSdk = 30
        targetSdk = 34
        versionCode = gitCommitCount
        versionName = gitLatestTag
        // Latest commit hash as BuildConfig.COMMIT_HASH
        buildConfigField("String", "COMMIT_HASH", "\"$gitCommitHash\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_9
        targetCompatibility = JavaVersion.VERSION_1_9
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }

    packaging {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment:2.7.5")
    implementation("androidx.navigation:navigation-ui:2.7.5")
    implementation("androidx.preference:preference:1.2.1")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    annotationProcessor("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-runtime:2.6.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.hivemq:hivemq-mqtt-client:1.3.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.maltaisn:icondialog:3.3.0")
    implementation("com.maltaisn:iconpack-default:1.1.0")
    implementation("com.github.skydoves:colorpickerview:2.3.0")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.20"))
}