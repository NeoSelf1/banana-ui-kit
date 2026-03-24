import com.vanniktech.maven.publish.SonatypeHost

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.maven.publish) apply true
}

mavenPublishing {
    coordinates(
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