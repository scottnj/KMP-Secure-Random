# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) project for secure random number generation that targets multiple platforms: Android, iOS, Web (React), Desktop (JVM), and Server (Ktor). The project demonstrates how to share Kotlin code across all these platforms while maintaining platform-specific implementations where needed.

## Project Structure

- `/composeApp` - Compose Multiplatform applications with platform-specific folders (commonMain, androidMain, iosMain, jvmMain)
- `/iosApp` - iOS application entry point and SwiftUI code
- `/server` - Ktor server application
- `/shared` - Core business logic shared across all platforms with expect/actual pattern for platform-specific implementations
- `/webApp` - React web application consuming the Kotlin/JS library from shared module

## Build Commands

### Android Application
```shell
./gradlew :composeApp:assembleDebug
```

### Desktop (JVM) Application
```shell
./gradlew :composeApp:run
```

### Server
```shell
./gradlew :server:run
```

### Web Application
First, build the Kotlin/JS shared library:
```shell
./gradlew :shared:jsBrowserDevelopmentLibraryDistribution
```

Then run the React app:
```shell
cd webApp
npm install
npm run start
```

### iOS Application
Open `/iosApp` directory in Xcode or use IDE run configuration.

## Testing

Run all tests across platforms:
```shell
./gradlew allTests
```

Platform-specific tests:
- JVM: `./gradlew jvmTest`
- JS (browser): `./gradlew jsBrowserTest`
- iOS Simulator: `./gradlew iosSimulatorArm64Test`
- Android unit tests: `./gradlew testDebugUnitTest`

## Code Quality

- Lint: `./gradlew lint`
- All checks: `./gradlew check`

## Architecture Notes

The project uses the expect/actual mechanism for platform-specific implementations. Common interfaces are defined in `shared/src/commonMain` with platform-specific implementations in their respective folders (androidMain, iosMain, jvmMain, jsMain).

The server runs on port 8080 (defined in Constants.kt).

Core shared code includes platform abstraction layer and business logic that works across all targets.