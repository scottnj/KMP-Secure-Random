import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
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
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    )
    iosTargets.forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SecureRandom"
            isStatic = true
        }
    }

    // macOS targets
    val macosTargets = listOf(
        macosX64(),
        macosArm64()
    )

    // watchOS targets
    val watchosTargets = listOf(
        watchosArm32(),
        watchosArm64(),
        watchosX64(),
        watchosSimulatorArm64()
    )

    // tvOS targets
    val tvosTargets = listOf(
        tvosArm64(),
        tvosX64(),
        tvosSimulatorArm64()
    )

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

        // Create a custom hierarchy that separates watchOS from other Apple platforms
        // to avoid metadata compilation conflicts due to different bit width requirements

        val nativeMain by creating {
            dependsOn(commonMain.get())
        }

        val appleMain by creating {
            dependsOn(nativeMain)
        }

        // iOS, macOS, tvOS use appleMain (can share SecRandomCopyBytes APIs)
        val iosMain by creating {
            dependsOn(appleMain)
        }
        val macosMain by creating {
            dependsOn(appleMain)
        }
        val tvosMain by creating {
            dependsOn(appleMain)
        }

        // watchOS depends directly on commonMain to avoid any bit width conflicts
        val watchosMain by creating {
            dependsOn(commonMain.get())
        }

        // Configure other native platforms
        val linuxMain by creating {
            dependsOn(nativeMain)
        }
        val mingwMain by creating {
            dependsOn(nativeMain)
        }
        // Android Native per-architecture source sets (isolated like watchOS to avoid metadata conflicts)
        val androidNativeArm32Main by getting {
            dependsOn(commonMain.get())
        }
        val androidNativeArm64Main by getting {
            dependsOn(commonMain.get())
        }
        val androidNativeX86Main by getting {
            dependsOn(commonMain.get())
        }
        val androidNativeX64Main by getting {
            dependsOn(commonMain.get())
        }

        // Connect source sets to actual targets
        iosTargets.forEach { target ->
            target.compilations["main"].defaultSourceSet.dependsOn(iosMain)
        }
        macosTargets.forEach { target ->
            target.compilations["main"].defaultSourceSet.dependsOn(macosMain)
        }
        tvosTargets.forEach { target ->
            target.compilations["main"].defaultSourceSet.dependsOn(tvosMain)
        }
        watchosTargets.forEach { target ->
            target.compilations["main"].defaultSourceSet.dependsOn(watchosMain)
        }

        // Connect Linux targets
        linuxX64().compilations["main"].defaultSourceSet.dependsOn(linuxMain)
        linuxArm64().compilations["main"].defaultSourceSet.dependsOn(linuxMain)

        // Connect Windows targets
        mingwX64().compilations["main"].defaultSourceSet.dependsOn(mingwMain)

        // Connect Android Native targets to per-architecture source sets
        androidNativeArm32().compilations["main"].defaultSourceSet.dependsOn(androidNativeArm32Main)
        androidNativeArm64().compilations["main"].defaultSourceSet.dependsOn(androidNativeArm64Main)
        androidNativeX86().compilations["main"].defaultSourceSet.dependsOn(androidNativeX86Main)
        androidNativeX64().compilations["main"].defaultSourceSet.dependsOn(androidNativeX64Main)
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
                rule("Minimum line coverage rate in percent") {
                    minBound(90)
                }
                rule("Minimum branch coverage rate in percent") {
                    minBound(85)
                }
            }
        }
    }
}

dependencyCheck {
    outputDirectory = "${layout.buildDirectory.get()}/reports/dependency-check"
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
    suppressionFile = "$projectDir/dependency-check-suppressions.xml"

    // Configure to use Gradle's dependency resolution instead of re-downloading
    // This prevents redundant Maven Central downloads and timeouts
    scanConfigurations = listOf(
        "jvmRuntimeClasspath",
        "jvmCompileClasspath",
        "androidDebugRuntimeClasspath",
        "androidReleaseRuntimeClasspath"
    )

    analyzers {
        // Disable Central Analyzer - we're using Gradle's dependency resolution instead
        // This prevents redundant downloads from Maven Central
        centralEnabled = false
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
        // Try to get API key from multiple sources in priority order:
        // 1. Environment variable (CI/CD friendly)
        // 2. local.properties (secure local development - not in git)
        // 3. gradle.properties (if you choose to add it there)
        // 4. Empty string (will work but be slower)
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        val nvdApiKey = System.getenv("NVD_API_KEY")
            ?: localProperties.getProperty("nvd.api.key")
            ?: project.findProperty("nvd.api.key")?.toString()
            ?: ""

        apiKey = nvdApiKey
        delay = if (nvdApiKey.isNotEmpty() && nvdApiKey != "YOUR_NVD_API_KEY_HERE") 3000 else 16000  // Faster with valid API key
        maxRetryCount = 10
        validForHours = 24

        if (nvdApiKey.isEmpty() || nvdApiKey == "YOUR_NVD_API_KEY_HERE") {
            println("⚠️ NVD API key not configured. Dependency checks will be slower.")
            println("   Add your key to local.properties: nvd.api.key=your-key-here")
            println("   Get a free key at: https://nvd.nist.gov/developers/request-an-api-key")
        } else {
            println("✅ NVD API key configured successfully (using ${if (System.getenv("NVD_API_KEY") != null) "environment variable" else "local.properties"})")
        }
    }
    failBuildOnCVSS = 7.0f
}

// Dokka configuration for API documentation generation
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleName.set("KMP-Secure-Random")
    outputDirectory.set(layout.buildDirectory.dir("dokka"))

    dokkaSourceSets {
        configureEach {
            includeNonPublic.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressObviousFunctions.set(true)

            perPackageOption {
                matchingRegex.set(".*\\.internal.*")
                suppress.set(true)
            }
        }
    }
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
    }
}

// ==================== Statistical Testing Tasks ====================

// NIST SP 800-22 Core Tests
val nistCoreTestsTask = tasks.register<Test>("nistCoreTests") {
    group = "verification"
    description = "Run NIST SP 800-22 Core statistical tests (5 tests)"
    testClassesDirs = tasks.named<Test>("jvmTest").get().testClassesDirs
    classpath = tasks.named<Test>("jvmTest").get().classpath

    filter {
        includeTestsMatching("com.scottnj.kmp_secure_random.nist.NistSP80022CoreTests")
    }

    doFirst {
        println("=" .repeat(70))
        println("Running NIST SP 800-22 Core Tests")
        println("=" .repeat(70))
        println("Tests: Frequency within Block, Runs, Longest Run, Binary Matrix Rank, Cumulative Sums")
        println("Significance level: α = 0.01 (99% confidence)")
        println("=" .repeat(70))
    }
}

// NIST SP 800-22 Advanced Tests
val nistAdvancedTestsTask = tasks.register<Test>("nistAdvancedTests") {
    group = "verification"
    description = "Run NIST SP 800-22 Advanced statistical tests (5 tests)"
    testClassesDirs = tasks.named<Test>("jvmTest").get().testClassesDirs
    classpath = tasks.named<Test>("jvmTest").get().classpath

    filter {
        includeTestsMatching("com.scottnj.kmp_secure_random.nist.NistSP80022AdvancedTests")
    }

    doFirst {
        println("=" .repeat(70))
        println("Running NIST SP 800-22 Advanced Tests")
        println("=" .repeat(70))
        println("Tests: DFT Spectral, Approximate Entropy, Serial, Linear Complexity, Maurer's Universal")
        println("Significance level: α = 0.01 (99% confidence)")
        println("Note: Linear Complexity test currently disabled pending calibration")
        println("=" .repeat(70))
    }
}

// All NIST Tests
tasks.register("nistTests") {
    group = "verification"
    description = "Run all NIST SP 800-22 statistical tests (core + advanced)"

    dependsOn(nistCoreTestsTask, nistAdvancedTestsTask)

    doFirst {
        println("=" .repeat(70))
        println("Running Complete NIST SP 800-22 Test Suite")
        println("=" .repeat(70))
    }
}

// FIPS 140-2 Compliance Tests
tasks.register<Test>("fipsTests") {
    group = "verification"
    description = "Run FIPS 140-2 compliance tests (4 required tests + full compliance)"
    testClassesDirs = tasks.named<Test>("jvmTest").get().testClassesDirs
    classpath = tasks.named<Test>("jvmTest").get().classpath

    filter {
        includeTestsMatching("com.scottnj.kmp_secure_random.fips.FIPS1402ComplianceTests")
    }

    doFirst {
        println("=" .repeat(70))
        println("Running FIPS 140-2 Compliance Tests")
        println("=" .repeat(70))
        println("Tests: Monobit, Poker, Runs, Long Run")
        println("Test sequence: 20,000 bits per iteration")
        println("=" .repeat(70))
    }
}

// Full compliance report
tasks.register<Test>("complianceReport") {
    group = "verification"
    description = "Generate comprehensive statistical compliance report (NIST + FIPS)"
    testClassesDirs = tasks.named<Test>("jvmTest").get().testClassesDirs
    classpath = tasks.named<Test>("jvmTest").get().classpath

    filter {
        includeTestsMatching("com.scottnj.kmp_secure_random.nist.*")
        includeTestsMatching("com.scottnj.kmp_secure_random.fips.*")
    }

    doFirst {
        println("=" .repeat(70))
        println("COMPREHENSIVE STATISTICAL COMPLIANCE REPORT")
        println("=" .repeat(70))
        println("Generating full randomness quality assessment...")
        println("=" .repeat(70))
    }

    doLast {
        println()
        println("=" .repeat(70))
        println("COMPLIANCE REPORT COMPLETE")
        println("=" .repeat(70))
        println("View detailed results at:")
        println("  file://${reports.html.outputLocation.get()}/index.html")
        println("=" .repeat(70))
    }
}

// Quality gates task that enforces all quality standards
tasks.register("qualityGates") {
    group = "verification"
    description = "Runs comprehensive quality gates including tests, coverage, static analysis, and security checks"

    // Define dependencies on all quality checks
    dependsOn(
        tasks.named("allTests"),
        tasks.named("koverVerify"),
        tasks.named("detekt"),
        tasks.named("dependencyCheckAnalyze"),
        tasks.named("dokkaHtml")
    )

    doLast {
        println("🎯 All Quality Gates PASSED! ✅")
        println("   📋 Tests: All platforms tested")
        println("   📊 Coverage: >90% line coverage achieved")
        println("   🔍 Static Analysis: Clean detekt scan")
        println("   🛡️  Security: OWASP dependency check passed")
        println("   📖 Documentation: Generated successfully")
        println("   🚀 Project ready for production!")
    }
}

// Enhanced check task that includes all quality gates
tasks.named("check") {
    dependsOn(tasks.named("qualityGates"))
}

// Developer-friendly quick check task for local development
tasks.register("quickCheck") {
    group = "verification"
    description = "Quick quality checks for local development (tests, coverage, detekt)"

    dependsOn(
        tasks.named("jvmTest"),
        tasks.named("koverVerify"),
        tasks.named("detekt")
    )

    doLast {
        println("⚡ Quick Quality Check PASSED! ✅")
        println("   🧪 JVM Tests: Passed")
        println("   📊 Coverage: >90% achieved")
        println("   🔍 Static Analysis: Clean")
        println("   💻 Ready for local development!")
    }
}

// Smoke test task for Dokka documentation generation plugin
tasks.register("dokkaSmokeTest") {
    group = "verification"
    description = "Smoke test to verify Dokka documentation generation actually works"

    // Make this task depend on dokkaHtml to actually test generation
    dependsOn(tasks.named("dokkaHtml"))

    // Configuration cache compatible approach - capture at configuration time
    val buildFile = file("build.gradle.kts")
    val commonMainDir = file("src/commonMain/kotlin")
    val outputDir = layout.buildDirectory.dir("dokka")

    doLast {
        println("📚 Running Dokka smoke test...")

        // 1. Verify the plugin is loaded and tasks are available
        val allTaskNames = tasks.names
        val dokkaTasks = allTaskNames.filter { it.startsWith("dokka") }
        require(dokkaTasks.isNotEmpty()) {
            "❌ Dokka plugin not properly loaded - no dokka tasks found"
        }

        // 2. Verify expected tasks are present
        val expectedTasks = listOf("dokkaHtml")
        val missingTasks = expectedTasks.filter { it !in dokkaTasks }
        require(missingTasks.isEmpty()) {
            "❌ Missing Dokka tasks: $missingTasks"
        }

        // 3. Verify dokka actually generated documentation files
        val outputDirectory = outputDir.get().asFile
        require(outputDirectory.exists() && outputDirectory.isDirectory) {
            "❌ Dokka output directory not found: ${outputDirectory.absolutePath}"
        }

        val indexHtml = File(outputDirectory, "index.html")
        require(indexHtml.exists() && indexHtml.isFile) {
            "❌ Dokka did not generate index.html: ${indexHtml.absolutePath}"
        }

        val navigationHtml = File(outputDirectory, "navigation.html")
        require(navigationHtml.exists() && navigationHtml.isFile) {
            "❌ Dokka did not generate navigation.html: ${navigationHtml.absolutePath}"
        }

        // 4. Verify HTML content contains expected documentation
        val indexContent = indexHtml.readText()
        require(indexContent.contains("KMP-Secure-Random")) {
            "❌ Generated documentation does not contain module name 'KMP-Secure-Random'"
        }
        require(indexContent.contains("html") && indexContent.contains("</html>")) {
            "❌ Generated index.html is not valid HTML"
        }

        // 5. Verify configuration file structure
        require(buildFile.exists()) {
            "❌ Build configuration file not found"
        }

        val buildContent = buildFile.readText()
        require(buildContent.contains("dokka")) {
            "❌ Dokka configuration not found in build.gradle.kts"
        }

        // 6. Verify source sets exist for documentation
        require(commonMainDir.exists()) {
            "❌ Common main source directory not found: ${commonMainDir.absolutePath}"
        }

        println("✅ Dokka smoke test PASSED - Documentation actually generated!")
        println("   📋 Available tasks: ${dokkaTasks.sorted()}")
        println("   📁 Output directory: ${outputDirectory.absolutePath}")
        println("   📄 Generated files: index.html (${indexHtml.length()} bytes), navigation.html (${navigationHtml.length()} bytes)")
        println("   📂 Source directory: ${commonMainDir.absolutePath}")
        println("   ⚙️  Configuration validated in build.gradle.kts")
        println("   📖 Dokka successfully generated HTML documentation")
    }
}
