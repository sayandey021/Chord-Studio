import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)

    // Kotlin & Coroutines
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.coroutines.swing)
    implementation(libs.org.jetbrains.kotlinx.serialization.json)
    implementation(libs.google.code.gson)

    // Chords rendering
    implementation(libs.chrynan.chords.compose)

    // SQLite driver for desktop database
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // On-Device MP3 & Audio Decoding
    implementation("javazoom:jlayer:1.0.1")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")

    // On-Device Neural Network Inference for SOTA Chord Recognition
    implementation("com.microsoft.onnxruntime:onnxruntime:1.17.1")

    // PDF Generation for Tab Exports
    implementation("org.apache.pdfbox:pdfbox:2.0.31")

    // Modern Native File Picker & Saver (Windows IFileDialog, macOS NSOpenPanel, Linux)
    implementation(libs.filekit.core)
    implementation(libs.filekit.compose)

    // Windows 11 DWM Native Theming & Dark Mode Integration
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.gbros.tabslite.desktop.MainKt"



        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Deb,
                TargetFormat.Rpm
            )
            packageName = "ChordStudio"
            packageVersion = "0.9.2"
            description = "Chord Studio"
            copyright = "© 2026 SaayanSoft"
            vendor = "SaayanSoft"

            windows {
                menuGroup = "Chord Studio"
                upgradeUuid = "8b23c91d-4874-4b53-a55e-49b81b241372"
                shortcut = true
                iconFile.set(project.file("src/main/resources/icon.ico"))
                dirChooser = true
                perUserInstall = false
                packageVersion = "0.9.2"
            }

            macOS {
                bundleID = "com.saayansoft.chordstudio"
                dockName = "Chord Studio"
                iconFile.set(project.file("src/main/resources/icon.png"))
                packageVersion = "1.0.0"
            }

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}
