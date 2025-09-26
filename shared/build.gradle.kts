import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

kotlin {
    // JVM target
    jvm()

    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release", "debug")
    }

    // iOS targets
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SecureRandom"
            isStatic = true
        }
    }

    // macOS targets
    macosX64()
    macosArm64()

    // watchOS targets
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()

    // tvOS targets
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    // JavaScript/Browser target
    js(IR) {
        outputModuleName = "kmp-secure-random"
        browser()
        nodejs()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
        }
    }

    // WASM target
    wasmJs {
        browser()
        nodejs()
        d8()
        binaries.library()
    }

    // Linux targets
    linuxX64()
    linuxArm64()

    // Windows targets
    mingwX64()

    // Native targets for embedded/other platforms
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.scottnj.kmp_secure_random.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    autoCorrect = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

kover {
    reports {
        total {
            html {
                onCheck = true
            }
            xml {
                onCheck = true
            }
            verify {
                onCheck = true
                rule {
                    minBound(20)
                }
            }
        }
    }
}
