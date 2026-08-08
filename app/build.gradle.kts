plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.rynekryz.rynotes"
    compileSdk = 37
    androidResources {
        localeFilters += "en"
    }
    defaultConfig {
        applicationId = "com.rynekryz.rynotes"
        minSdk = 31
        targetSdk = 36
        versionCode = 10
        versionName = "0.1.0-beta"
    }
    buildFeatures {
        compose = true
    	  buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abiFilter = output.filters.find {
                it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
            }?.identifier
            val abiName = when (abiFilter) {
                "arm64-v8a" -> "arm64"
                "armeabi-v7a" -> "armv7"
                "x86_64" -> "x86_64"
                else -> abiFilter ?: "universal"
            }
            val buildTypeName = variant.buildType ?: "debug"
            output.outputFileName.set("rynotes-0.0.1-alpha-$abiName-$buildTypeName.apk")
        }
    }
}

dependencies {
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha25")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}