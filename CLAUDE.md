# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) library for secure random number generation. The library targets ALL possible KMP platforms including JVM, Android, iOS, macOS, watchOS, tvOS, JavaScript, WASM, Linux, Windows, and Android Native variants. This is a pure library project with no UI components.

**Repository**: https://github.com/scottnj/KMP-Secure-Random

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
- Windows (MinGW): `./gradlew mingwX64Test`
- Linux: `./gradlew linuxX64Test`

### Code Quality

#### Quality Gates & Comprehensive Checks
- **Full quality gates**: `./gradlew qualityGates` - Runs all quality checks (tests, coverage, static analysis, security, docs)
- **Enhanced check**: `./gradlew check` - Comprehensive verification including quality gates
- **Quick check**: `./gradlew quickCheck` - Fast local development checks (JVM tests, coverage, detekt)

#### Individual Quality Tools
- **Static analysis**: `./gradlew detekt` - Kotlin code quality and security analysis
- **Code coverage report**: `./gradlew koverHtmlReport` - Generate HTML coverage report
- **Coverage verification**: `./gradlew koverVerify` - Verify >90% line coverage, >85% branch coverage
- **Security scan**: `./gradlew dependencyCheckAnalyze` - OWASP dependency vulnerability scan
- **Documentation**: `./gradlew dokkaHtml` - Generate API documentation

#### Smoke Tests & Validation
- **OWASP smoke test**: `./gradlew owaspDependencyCheckSmokeTest` - Verify security scanner setup
- **Dokka smoke test**: `./gradlew dokkaSmokeTest` - Verify documentation generation
- **Gradle sync**: `./gradlew --refresh-dependencies` - Refresh dependency cache

## Architecture Notes

The library uses Kotlin Multiplatform's expect/actual mechanism for platform-specific secure random implementations:

- **Common interface**: `SecureRandom` interface defined in `commonMain` with `createSecureRandom()` factory function
- **Platform implementations**: Each target uses platform-specific secure random APIs via expect/actual pattern
- **Current Status**: 12 of 12 platforms fully implemented with production-ready secure random generation (100% platform coverage)
- **Test framework**: Comprehensive test suite with 31 test files covering statistical validation, security, and performance

## Current Implementation Status

**[x] Infrastructure**: Kermit logging, Detekt analysis, Kover coverage, OWASP security scanning, Dokka documentation, automated quality gates

**[x] Security Framework**: `@AllowInsecureFallback` opt-in system, `FallbackPolicy` enum, secure-by-default behavior

**[x] Platform Implementation (12/12 Complete):**
- **JVM, Android, iOS, macOS, tvOS**: Production-ready with platform-specific secure random adapters
- **watchOS**: Isolated implementation using `arc4random()` (architectural separation)
- **JavaScript**: Web Crypto API with Node.js crypto (secure-only, no insecure fallbacks)
- **WASM-JS**: Web Crypto API with explicit opt-in for Math.random fallback (security-enhanced)
- **Linux**: `getrandom()` syscall + `/dev/urandom` fallback (GitHub Actions validated)
- **Windows**: `CryptGenRandom` API (GitHub Actions validated)
- **Android Native**: Production-ready with per-architecture implementation (GitHub Actions validated)

**[x] Quality Metrics**: 35+ test files, comprehensive fallback policy testing, zero static analysis violations, comprehensive CI/CD pipeline

## Production Architecture

**Clean Architecture**: Domain layer with `SecureRandom` interface, application layer with validation logic, infrastructure layer with platform-specific adapters

**Error Handling**: `Result<T>` pattern with custom exception hierarchy (`SecureRandomException`, `SecureRandomInitializationException`, `SecureRandomGenerationException`, `InvalidParameterException`)

**Quality Assurance**: Kermit logging, Detekt analysis, Kover coverage, OWASP scanning, Dokka documentation, performance benchmarks

**Implementation Strategy**: Adapter pattern, comprehensive testing (statistical/security/performance), guaranteed thread-safety, secure memory handling

## Security Framework

**Secure Fallback Policy System**: Enhanced security through explicit opt-in for insecure fallbacks

### Factory Functions

**Secure by Default (Recommended)**:
```kotlin
// Always secure - fails if secure random unavailable
val secureRandom = createSecureRandom().getOrThrow()
```

**Explicit Insecure Fallback (Use with Caution)**:
```kotlin
@OptIn(AllowInsecureFallback::class)
val secureRandom = createSecureRandom(FallbackPolicy.ALLOW_INSECURE).getOrThrow()
```

### Platform Security Characteristics

| Platform | Primary Method | Secure Fallback | Insecure Fallback |
|----------|---------------|-----------------|-------------------|
| **JVM** | `SecureRandom` (various algorithms) | ✅ Multiple secure providers | ❌ None |
| **Android** | `SHA1PRNG`/`NativePRNG` | ✅ Secure provider chain | ❌ None |
| **iOS/macOS/tvOS** | `SecRandomCopyBytes` | ✅ None needed | ❌ None |
| **watchOS** | `arc4random` | ✅ None needed | ❌ None |
| **JavaScript** | Web Crypto API / Node.js crypto | ✅ None needed | ❌ None |
| **WASM-JS** | Web Crypto API | ❌ None available | ⚠️ Math.random (opt-in only) |
| **Linux/Android Native** | `getrandom()` syscall | ✅ `/dev/urandom` | ❌ None |
| **Windows** | `CryptGenRandom` API | ✅ None needed | ❌ None |

### Security Policy Details

- **`FallbackPolicy.SECURE_ONLY`** (Default): Only cryptographically secure methods allowed
- **`FallbackPolicy.ALLOW_INSECURE`**: Permits insecure fallbacks with explicit `@OptIn(AllowInsecureFallback)`
- **Compiler Warnings**: `@RequiresOptIn` ensures developers understand security implications
- **Platform Enforcement**: Only WASM-JS platform has insecure fallbacks; others ignore policy safely

## Implementation Roadmap

**[x] Phase 1-2 Complete**: JVM-first foundation, platform expansion (12/12 platforms implemented)

**[ ] Phase 3 - Production Readiness**:
- [x] CI/CD pipeline and documentation
- [x] Android Native implementation (per-architecture source sets)
- [ ] Enhanced Statistical Testing:
  - [ ] Implement full NIST SP 800-22 test suite (15 statistical tests)
  - [ ] Add runs test, longest run test, rank test, overlapping template test
  - [ ] Create FIPS 140-2 compliance validation
  - [ ] Add continuous monitoring for randomness quality degradation
- [ ] Security audit and penetration testing
- [ ] Maven Central publishing setup

## Development Progress

**[x] Completed Phases**:
- **Phase 1-4**: Foundation, JVM implementation, comprehensive testing, quality assurance tooling
- **Phase 5**: Platform expansion (12/12 platforms complete)
- **Phase 6**: Documentation, licensing, CI/CD pipeline

**[ ] Remaining Work**:
- Enhanced statistical testing (NIST SP 800-22 test suite)
- Security audit and penetration testing
- Maven Central publishing setup

### Android Native Implementation [x]

**Problem**: Different syscall numbers and bit widths across architectures cause KMP metadata compilation conflicts.

**Solution**: Per-architecture source sets following watchOS pattern:
- `androidNativeArm32Main` - ARM32 syscall #384, 32-bit UInt types
- `androidNativeArm64Main` - ARM64 syscall #278, 64-bit ULong types
- `androidNativeX86Main` - x86 syscall #355, 32-bit UInt types
- `androidNativeX64Main` - x86_64 syscall #318, 64-bit ULong types

Each using `getrandom()` + `/dev/urandom` fallback pattern.

**Result**: Complete architectural isolation prevents metadata conflicts while providing production-ready secure random generation for all Android Native architectures. GitHub Actions workflow validates cross-compilation and syscall configurations.

**Overall Project Completion: 100%** - 12/12 platforms implemented

## Platform Implementation Notes

### watchOS Architectural Separation [x]
**Issue**: Bit width conflicts in KMP metadata compilation with other Apple platforms
**Solution**: Isolated `watchosMain` source set using `arc4random()`, separate from `appleMain` using `SecRandomCopyBytes`
**Result**: Complete isolation prevents metadata conflicts while maintaining platform-specific optimizations

### JavaScript/WASM-JS Environment Detection [x]
**JavaScript**: Automatic detection between Web Crypto API (browsers) and Node.js crypto
**WASM-JS**: Web Crypto API for browsers, enhanced Math.random fallback for D8 testing environments
**Innovation**: Multi-source XOR technique passes statistical tests in constrained environments

### Native Platforms GitHub Actions Validation [x]
**Linux**: `LinuxSecureRandomAdapter` using `getrandom()` syscall + `/dev/urandom` fallback, validated on Ubuntu
**Windows**: `WindowsSecureRandom` using `CryptGenRandom` API, validated on Windows Server
**Result**: Production-ready native implementations confirmed on real machines

## Recent Achievements [x]

**Security Framework**: Implemented secure fallback policy system with `@AllowInsecureFallback` opt-in mechanism for explicit security control

**WASM-JS Security Design**: Secure-by-default behavior with explicit opt-in for Math.random fallback, includes compiler warnings for enhanced security awareness

**Comprehensive Testing**: Added fallback policy test coverage across all platforms, validating secure-by-default behavior and insecure opt-in functionality

**Native Platform Validation**: Linux, Windows, and Android Native implementations validated on GitHub Actions with real platform cryptographic APIs

**Production Readiness**: 12/12 platforms complete with enhanced security framework, comprehensive documentation, MIT licensing, CI/CD pipeline

**Security Confirmation**: All implemented platforms use platform-native cryptographic APIs with explicit security boundary enforcement

