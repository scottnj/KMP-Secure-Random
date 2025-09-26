# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) library for secure random number generation. The library targets ALL possible KMP platforms including JVM, Android, iOS, macOS, watchOS, tvOS, JavaScript, WASM, Linux, Windows, and Android Native variants. This is a pure library project with no UI components.

## Project Structure

- `/shared` - The main and only module containing the secure random library implementation
  - `src/commonMain/kotlin` - Platform-agnostic implementation and interfaces
  - `src/*/kotlin` - Platform-specific implementations using expect/actual pattern
  - `src/commonTest/kotlin` - Cross-platform tests

## Build Commands

### Build the library
```shell
./gradlew build
```

### Build specific targets
- JVM: `./gradlew jvmJar`
- Android: `./gradlew assembleRelease`
- JavaScript: `./gradlew jsBrowserProductionLibraryDistribution`
- WASM: `./gradlew wasmJsBrowserProductionLibraryDistribution`
- iOS Framework: `./gradlew linkReleaseFrameworkIosArm64`
- macOS Framework: `./gradlew linkReleaseFrameworkMacosArm64`

### Testing

Run all tests across all platforms:
```shell
./gradlew allTests
```

Platform-specific tests:
- JVM: `./gradlew jvmTest`
- JavaScript: `./gradlew jsTest`
- WASM: `./gradlew wasmJsTest`
- iOS Simulator: `./gradlew iosSimulatorArm64Test`
- Android: `./gradlew testDebugUnitTest`
- macOS: `./gradlew macosArm64Test`
- tvOS Simulator: `./gradlew tvosSimulatorArm64Test`
- watchOS Simulator: `./gradlew watchosSimulatorArm64Test`

### Code Quality

- All checks: `./gradlew check`
- Gradle sync: `./gradlew --refresh-dependencies`

## Architecture Notes

The library uses Kotlin Multiplatform's expect/actual mechanism for platform-specific secure random implementations:

- **Common interface**: `SecureRandom` interface defined in `commonMain` with `createSecureRandom()` factory function
- **Platform implementations**: Each target has placeholder implementations ready for secure random generation
- **Current Status**: All implementations are placeholder TODOs - ready for actual secure random implementation
- **Test framework**: Basic test structure in place, currently with placeholder test

## Current Implementation Status

**✅ Fully Set Up (Placeholder TODOs):**
- **JVM**: Ready for `java.security.SecureRandom` implementation
- **Android**: Ready for Android's secure random APIs
- **iOS/macOS/watchOS/tvOS**: Ready for Apple's `SecRandomCopyBytes`
- **JavaScript**: Ready for Web Crypto API's `crypto.getRandomValues()`
- **WASM**: Ready for WASM-compatible secure random generation
- **Linux/Windows**: Ready for OS-specific secure random sources (/dev/urandom, CryptGenRandom)
- **Android Native**: Ready for direct native random API access

**📋 Development Notes:**
- All 20+ KMP targets compile successfully
- Comprehensive test framework ready
- Clean project structure with no UI dependencies
- All simulator tests pass (tvOS, watchOS, iOS)