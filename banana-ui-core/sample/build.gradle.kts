import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish) apply true
}

android {
    namespace = "com.neon.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neon.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

mavenPublishing {
    coordinates( // Coordinate(GAV)
        groupId = "io.github.neoself1",
        artifactId = "banana-ui-kit",
        version = "1.0.0"
    )

    pom {
        name.set("banana-ui-kit") // Project Name
        description.set("Lightweight Android UI library that provides polished, iOS-style components.") // Project Description
        inceptionYear.set("2026") // 개시년도
        url.set("https://github.com/NeoSelf1/banana-ui-kit") // Project URL

        licenses { // License Information
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers { // Developer Information
            developer {
                id.set("neoself1")
                name.set("Hyeongseok Kim")
                email.set("neoself1105@gmail.com")
            }
        }

        scm { // SCM Information
            connection.set("scm:git:git://github.com/neoself1/banana-ui-kit.git")
            developerConnection.set("scm:git:ssh://github.com/neoself1/banana-ui-kit.git")
            url.set("https://github.com/neoself1/banana-ui-kit.git")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications() // GPG/PGP 서명
}

dependencies {
    implementation(project(":banana-ui-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.ui.test.uiautomator)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
