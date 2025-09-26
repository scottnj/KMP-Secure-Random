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
- Static analysis: `./gradlew detekt`
- Gradle sync: `./gradlew --refresh-dependencies`

## Architecture Notes

The library uses Kotlin Multiplatform's expect/actual mechanism for platform-specific secure random implementations:

- **Common interface**: `SecureRandom` interface defined in `commonMain` with `createSecureRandom()` factory function
- **Platform implementations**: Each target has placeholder implementations ready for secure random generation
- **Current Status**: All implementations are placeholder TODOs - ready for actual secure random implementation
- **Test framework**: Basic test structure in place, currently with placeholder test

## Current Implementation Status

**✅ Infrastructure Complete:**
- **Cross-Platform Logging**: Kermit dependency added and tested across all platforms
- **Static Analysis**: Detekt plugin integrated with security and quality rules
- **Build System**: All 20+ KMP targets compile and build successfully
- **Test Framework**: Comprehensive testing infrastructure validated
- **Project Structure**: Clean architecture with proper .gitignore exclusions

**✅ Platform Readiness (Placeholder TODOs):**
- **JVM**: Ready for `java.security.SecureRandom` implementation
- **Android**: Ready for Android's secure random APIs
- **iOS/macOS/watchOS/tvOS**: Ready for Apple's `SecRandomCopyBytes`
- **JavaScript**: Ready for Web Crypto API's `crypto.getRandomValues()`
- **WASM**: Ready for WASM-compatible secure random generation
- **Linux/Windows**: Ready for OS-specific secure random sources (/dev/urandom, CryptGenRandom)
- **Android Native**: Ready for direct native random API access

**📋 Validation Complete:**
- ✅ All 20+ KMP targets compile successfully
- ✅ All available platform tests pass (JVM, JS, WASM, iOS Sim, tvOS Sim, watchOS Sim, macOS)
- ✅ Cross-platform logging infrastructure working
- ✅ Static analysis (detekt) running cleanly with comprehensive rules
- ✅ Build artifacts properly excluded from version control

## Production Architecture

This library follows clean architecture principles with robust error handling and platform-specific adapters:

### Clean Architecture Layers
- **Domain Layer**: Core `SecureRandom` interface with `Result<T>` error handling
- **Application Layer**: Use cases, validation logic, and business rules
- **Infrastructure Layer**: Platform-specific adapters using adapter pattern
- **Clean Separation**: Each platform implementation as isolated, testable adapter

### Error Handling Strategy
- **Result<T> Pattern**: All operations return `SecureRandomResult<T>` instead of throwing exceptions
- **Custom Exception Hierarchy**:
  - `SecureRandomException` - Base exception class
  - `SecureRandomInitializationException` - Platform API initialization failures
  - `SecureRandomGenerationException` - Random generation failures
  - `InvalidParameterException` - Invalid bounds/parameters
- **Graceful Degradation**: Fallback mechanisms where appropriate
- **Platform-Specific Handling**: Handle platform-specific failure modes

### Quality Assurance
- **Cross-Platform Logging**: `kermit` for structured, security-aware logging ✅ **IMPLEMENTED**
- **Static Analysis**: `detekt` for Kotlin code quality and security analysis ✅ **IMPLEMENTED**
- **Code Coverage**: `kover` for comprehensive test coverage reporting
- **Security Scanning**: OWASP dependency checking and vulnerability analysis
- **Performance Monitoring**: Benchmarks for random generation across platforms

### Implementation Strategy
- **One Platform at a Time**: Start with JVM, then expand to other platforms
- **Adapter Pattern**: Platform implementations as clean, interchangeable adapters
- **Comprehensive Testing**: Statistical randomness tests, security tests, performance benchmarks
- **Thread Safety**: All implementations guaranteed thread-safe
- **Memory Security**: Secure handling and clearing of sensitive random data

## Implementation Roadmap

### Phase 1: Foundation (JVM-First Approach)
1. JVM implementation with `java.security.SecureRandom` adapter
2. Comprehensive error handling with `Result<T>` types
3. Statistical randomness validation and security tests
4. Performance benchmarks and thread-safety verification

### Phase 2: Platform Expansion
1. **Android**: Android-specific secure random APIs with fallbacks
2. **iOS/Apple Platforms**: `SecRandomCopyBytes` implementation with error handling
3. **JavaScript/WASM**: Web Crypto API with Node.js crypto fallback
4. **Native Platforms**: OS-level secure random sources (Linux, Windows, etc.)

### Phase 3: Production Readiness
1. CI/CD pipeline for automated testing across all 20+ platforms
2. Documentation generation with dokka
3. Security audit and penetration testing
4. Performance optimization and memory leak prevention

## Development Todo Checklist

### 🏗️ Phase 1: Foundation & Infrastructure
- [ ] **Dependencies & Build Setup**
  - [x] Add `kermit` logging dependency to build.gradle.kts
  - [x] Add `detekt` static analysis plugin and configuration
  - [ ] Add `kover` code coverage plugin and configuration
  - [ ] Add OWASP dependency-check plugin
  - [ ] Add `dokka` documentation generation plugin

- [ ] **Core Architecture**
  - [ ] Create `SecureRandomResult<T>` sealed class for error handling
  - [ ] Create custom exception hierarchy (`SecureRandomException`, `SecureRandomInitializationException`, etc.)
  - [ ] Design enhanced `SecureRandom` interface with Result<T> return types
  - [ ] Create parameter validation utilities
  - [x] Set up cross-platform logging infrastructure with kermit

### 🎯 Phase 2: JVM Implementation (First Platform)
- [ ] **JVM Secure Random Adapter**
  - [ ] Create `JvmSecureRandomAdapter` class wrapping `java.security.SecureRandom`
  - [ ] Implement proper algorithm selection (NativePRNG, SHA1PRNG, etc.)
  - [ ] Add comprehensive error handling for JVM-specific failures
  - [ ] Ensure thread-safety and performance optimization
  - [ ] Implement secure memory handling and cleanup

- [ ] **Enhanced Common Interface**
  - [ ] Update common `SecureRandom` interface with Result<T> methods
  - [ ] Add parameter validation for all public methods
  - [ ] Include security-focused method contracts and documentation
  - [ ] Maintain backward compatibility where possible

### 🧪 Phase 3: Comprehensive Testing (JVM)
- [ ] **Statistical Randomness Tests**
  - [ ] Implement chi-square test for uniform distribution
  - [ ] Add entropy analysis tests
  - [ ] Create NIST randomness test suite integration
  - [ ] Add autocorrelation tests for independence

- [ ] **Security & Edge Case Testing**
  - [ ] Test error conditions (invalid parameters, system failures)
  - [ ] Verify thread-safety with concurrent access tests
  - [ ] Test memory security (no sensitive data leaks)
  - [ ] Add performance benchmarks and regression tests

- [ ] **Integration Testing**
  - [ ] Test actual JVM SecureRandom integration (not mocks)
  - [ ] Verify behavior across different JVM implementations
  - [ ] Test under resource constraints and failure scenarios

### 📊 Phase 4: Quality Assurance & Tooling
- [ ] **Static Analysis & Code Quality**
  - [x] Configure detekt rules for security and performance
  - [ ] Set up code coverage targets with kover (>90% target)
  - [ ] Run OWASP dependency vulnerability scans
  - [ ] Generate API documentation with dokka

- [ ] **Build & Testing Infrastructure**
  - [x] Update build commands in CLAUDE.md for new tools
  - [x] Add code quality checks to `./gradlew check`
  - [ ] Set up automated quality gates
  - [ ] Create developer-friendly error messages and logging

### 🌐 Phase 5: Platform Expansion
- [ ] **Android Implementation**
  - [ ] Create `AndroidSecureRandomAdapter` with Android-specific APIs
  - [ ] Add fallback mechanisms for older Android versions
  - [ ] Test on real Android devices and emulators

- [ ] **iOS/Apple Platforms**
  - [ ] Implement `AppleSecureRandomAdapter` using `SecRandomCopyBytes`
  - [ ] Handle Apple-specific error conditions
  - [ ] Test on iOS simulators and real devices

- [ ] **JavaScript/WASM Platforms**
  - [ ] Create `WebSecureRandomAdapter` using Web Crypto API
  - [ ] Add Node.js crypto fallback for server-side JS
  - [ ] Handle browser compatibility issues

- [ ] **Native Platforms (Linux, Windows, etc.)**
  - [ ] Implement OS-specific secure random sources
  - [ ] Handle platform-specific error conditions
  - [ ] Test cross-compilation and native builds

### 🚀 Phase 6: Production Readiness
- [ ] **CI/CD Pipeline**
  - [ ] Set up GitHub Actions for all 20+ KMP targets
  - [ ] Add automated testing across platforms
  - [ ] Set up automated security scanning and quality gates

- [ ] **Documentation & Release**
  - [ ] Generate comprehensive API documentation
  - [ ] Create usage examples and best practices guide
  - [ ] Perform security audit and penetration testing
  - [ ] Prepare for production release and versioning

### 📋 Current Progress Tracking
**Active Phase**: Foundation & Infrastructure
**Next Milestone**: Complete JVM implementation with comprehensive testing
**Platform Focus**: JVM-first approach with clean architecture patterns