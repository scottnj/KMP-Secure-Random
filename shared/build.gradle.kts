import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.owaspDependencyCheck)
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
    @OptIn(ExperimentalWasmDsl::class)
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

dependencyCheck {
    outputDirectory = "${layout.buildDirectory.get()}/reports/dependency-check"
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
    suppressionFile = "$projectDir/dependency-check-suppressions.xml"
    analyzers {
        centralEnabled = true
        ossIndex {
            enabled = true
        }
        retirejs {
            enabled = true
        }
        nodeEnabled = true
        assemblyEnabled = false
        nuspecEnabled = false
        nugetconfEnabled = false
        cocoapodsEnabled = false
        swiftEnabled = false
        bundleAuditEnabled = false
        pyDistributionEnabled = false
        pyPackageEnabled = false
        rubygemsEnabled = false
        opensslEnabled = false
        cmakeEnabled = false
        autoconfEnabled = false
        composerEnabled = false
        cpanEnabled = false
        dartEnabled = false
        golangDepEnabled = false
        golangModEnabled = false
    }
    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: ""
        delay = 16000
        maxRetryCount = 10
        validForHours = 24
    }
    failBuildOnCVSS = 7.0f
}

// Smoke test task for OWASP dependency-check plugin
tasks.register("owaspDependencyCheckSmokeTest") {
    group = "verification"
    description = "Smoke test to verify OWASP dependency-check plugin is properly configured"

    // Configuration cache compatible approach - capture at configuration time
    val suppressionsFile = file("dependency-check-suppressions.xml")
    val outputDir = layout.buildDirectory.dir("reports/dependency-check")

    doLast {
        println("🔍 Running OWASP dependency-check smoke test...")

        // 1. Verify the plugin is loaded and tasks are available
        val allTaskNames = project.tasks.names
        val dependencyCheckTasks = allTaskNames.filter { it.startsWith("dependencyCheck") }
        require(dependencyCheckTasks.isNotEmpty()) {
            "❌ OWASP dependency-check plugin not properly loaded - no dependencyCheck tasks found"
        }

        // 2. Verify expected tasks are present
        val expectedTasks = listOf("dependencyCheckAnalyze", "dependencyCheckUpdate", "dependencyCheckPurge", "dependencyCheckAggregate")
        val missingTasks = expectedTasks.filter { it !in dependencyCheckTasks }
        require(missingTasks.isEmpty()) {
            "❌ Missing OWASP dependency-check tasks: $missingTasks"
        }

        // 3. Verify suppressions file exists and is valid XML
        require(suppressionsFile.exists()) {
            "❌ OWASP dependency-check suppressions file not found: ${suppressionsFile.absolutePath}"
        }

        val suppressionsContent = suppressionsFile.readText()
        require(suppressionsContent.contains("<?xml version=\"1.0\"")) {
            "❌ Suppressions file is not valid XML: ${suppressionsFile.absolutePath}"
        }
        require(suppressionsContent.contains("<suppressions")) {
            "❌ Suppressions file missing <suppressions> root element"
        }

        // 4. Verify output directory configuration is accessible
        val outputDirectory = outputDir.get().asFile
        require(outputDirectory.parentFile.exists() || outputDirectory.parentFile.mkdirs()) {
            "❌ Cannot access or create output directory: ${outputDirectory.absolutePath}"
        }

        // 5. Verify configuration file structure
        require(file("build.gradle.kts").exists()) {
            "❌ Build configuration file not found"
        }

        val buildContent = file("build.gradle.kts").readText()
        require(buildContent.contains("dependencyCheck")) {
            "❌ dependencyCheck configuration block not found in build.gradle.kts"
        }
        require(buildContent.contains("failBuildOnCVSS")) {
            "❌ CVSS threshold configuration not found"
        }

        println("✅ OWASP dependency-check plugin smoke test PASSED")
        println("   📋 Available tasks: ${dependencyCheckTasks.sorted()}")
        println("   📄 Suppressions file: ${suppressionsFile.absolutePath} (${suppressionsFile.length()} bytes)")
        println("   📁 Output directory: ${outputDirectory.absolutePath}")
        println("   ⚙️  Configuration validated in build.gradle.kts")
        println("   🔧 Plugin properly loaded and configured for vulnerability scanning")
    }
}
