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
- **Platform implementations**: Each target has placeholder implementations ready for secure random generation
- **Current Status**: All implementations are placeholder TODOs - ready for actual secure random implementation
- **Test framework**: Basic test structure in place, currently with placeholder test

## Current Implementation Status

**✅ Infrastructure Complete:**
- **Cross-Platform Logging**: Kermit dependency added and tested across all platforms
- **Static Analysis**: Detekt plugin integrated with security and quality rules
- **Code Coverage**: Kover plugin integrated with >90% line coverage verification (>85% branch coverage)
- **Dependency Security**: OWASP dependency-check plugin integrated for vulnerability scanning
- **Documentation Generation**: Dokka plugin integrated with HTML documentation generation and smoke testing
- **Build System**: All 20+ KMP targets compile and build successfully
- **Test Framework**: Comprehensive testing infrastructure validated with 6 test files covering all aspects
- **Quality Gates**: Automated quality gates with `qualityGates`, enhanced `check`, and `quickCheck` tasks
- **Project Structure**: Clean architecture with proper .gitignore exclusions

**✅ Platform Implementation Status:**
- **JVM**: ✅ **FULLY IMPLEMENTED** - Production-ready with `java.security.SecureRandom` adapter, thread-safe, all tests passing
- **Android**: ✅ **FULLY IMPLEMENTED** - Production-ready with `AndroidSecureRandomAdapter`, API level-aware algorithm selection, comprehensive testing
- **iOS/macOS/watchOS/tvOS**: 🔲 Ready for Apple's `SecRandomCopyBytes` (placeholder TODOs)
- **JavaScript**: 🔲 Ready for Web Crypto API's `crypto.getRandomValues()` (placeholder TODOs)
- **WASM**: 🔲 Ready for WASM-compatible secure random generation (placeholder TODOs)
- **Linux/Windows**: 🔲 Ready for OS-specific secure random sources (placeholder TODOs)
- **Android Native**: 🔲 Ready for direct native random API access (placeholder TODOs)

**📋 Validation Complete:**
- ✅ All 20+ KMP targets compile successfully
- ✅ All available platform tests pass (12 platforms: JVM, JS, WASM, iOS Sim, tvOS Sim, watchOS Sim, macOS, Android)
- ✅ Cross-platform logging infrastructure working
- ✅ Static analysis (detekt) running cleanly with comprehensive rules
- ✅ Code coverage >90% line coverage achieved with comprehensive test suite (6 test files, 80+ test methods)
- ✅ API documentation generation (dokka) with end-to-end smoke testing
- ✅ Quality gates enforcing all standards automatically
- ✅ Developer-friendly commands for local development workflow
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
- **Code Coverage**: `kover` for comprehensive test coverage reporting ✅ **IMPLEMENTED**
- **Security Scanning**: OWASP dependency-check for vulnerability analysis ✅ **IMPLEMENTED**
- **API Documentation**: `dokka` for comprehensive API documentation generation ✅ **IMPLEMENTED**
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
- [x] **Dependencies & Build Setup**
  - [x] Add `kermit` logging dependency to build.gradle.kts
  - [x] Add `detekt` static analysis plugin and configuration
  - [x] Add `kover` code coverage plugin and configuration
  - [x] Add OWASP dependency-check plugin
  - [x] Add `dokka` documentation generation plugin

- [x] **Core Architecture**
  - [x] Create `SecureRandomResult<T>` sealed class for error handling
  - [x] Create custom exception hierarchy (`SecureRandomException`, `SecureRandomInitializationException`, etc.)
  - [x] Design enhanced `SecureRandom` interface with Result<T> return types
  - [x] Create parameter validation utilities
  - [x] Set up cross-platform logging infrastructure with kermit

### 🎯 Phase 2: JVM Implementation (First Platform) **[COMPLETE]**
- [x] **JVM Secure Random Adapter**
  - [x] Create `JvmSecureRandomAdapter` class wrapping `java.security.SecureRandom`
  - [x] Implement proper algorithm selection (NativePRNG, SHA1PRNG, etc.)
  - [x] Add comprehensive error handling for JVM-specific failures
  - [x] Ensure thread-safety and performance optimization
  - [x] Implement secure memory handling and cleanup

- [x] **Enhanced Common Interface**
  - [x] Update common `SecureRandom` interface with Result<T> methods
  - [x] Add parameter validation for all public methods
  - [x] Include security-focused method contracts and documentation
  - [x] Maintain backward compatibility where possible

### ✅ Phase 3: Comprehensive Testing (JVM) **[COMPLETE]**
- [x] **Statistical Randomness Tests**
  - [x] Implement chi-square test for uniform distribution
  - [x] Add entropy analysis tests (Shannon entropy)
  - [x] Create NIST randomness test suite integration (5 NIST SP 800-22 tests)
  - [x] Add autocorrelation tests for independence

- [x] **Security & Edge Case Testing**
  - [x] Test error conditions (invalid parameters, system failures)
  - [x] Verify thread-safety with concurrent access tests
  - [x] Test memory security (no sensitive data leaks)
  - [x] Add performance benchmarks and regression tests

- [x] **Integration Testing**
  - [x] Test actual JVM SecureRandom integration (not mocks)
  - [x] Verify behavior across different JVM implementations
  - [x] Test under resource constraints and failure scenarios

- [x] **Cross-Platform Test Infrastructure**
  - [x] Move appropriate tests to `commonTest` for platform validation
  - [x] Create `BasicSecureRandomTest` (15 tests) for core API validation
  - [x] Create `StatisticalBasicTest` (7 tests) for cross-platform randomness validation
  - [x] Create `EdgeCaseTest` (9 tests) for parameter validation and edge cases
  - [x] Verify tests work on JVM and detect unimplemented platforms (JS validation)

### ✅ Phase 4: Quality Assurance & Tooling **[COMPLETE]**
- [x] **Static Analysis & Code Quality**
  - [x] Configure detekt rules for security and performance
  - [x] Set up code coverage targets with kover (>90% target achieved)
  - [x] Run OWASP dependency vulnerability scans
  - [x] Generate API documentation with dokka
  - [x] Configure NVD API key for faster security scans

- [x] **Build & Testing Infrastructure**
  - [x] Update build commands in CLAUDE.md for new tools
  - [x] Add code quality checks to `./gradlew check`
  - [x] Create OWASP dependency-check smoke test validation
  - [x] Create dokka smoke test with end-to-end validation
  - [x] Set up automated quality gates (`qualityGates`, enhanced `check`, `quickCheck`)
  - [x] Create developer-friendly error messages and logging
  - [x] Add comprehensive test suite (6 test files, 80+ test methods)

### 🌐 Phase 5: Platform Expansion
- [x] **Android Implementation** ✅ **COMPLETE**
  - [x] Create `AndroidSecureRandomAdapter` with Android-specific APIs
  - [x] Add fallback mechanisms for older Android versions
  - [x] Test on real Android devices and emulators

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

- [ ] **Enhanced Statistical Testing**
  - [ ] Implement full NIST SP 800-22 test suite (15 statistical tests)
  - [ ] Add runs test, longest run test, rank test, overlapping template test
  - [ ] Create FIPS 140-2 compliance validation
  - [ ] Add continuous monitoring for randomness quality degradation

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
**Completed Phases**:
- ✅ **Phase 1**: Foundation & Core Architecture
- ✅ **Phase 2**: JVM Implementation (First Platform) - **COMPLETE**
- ✅ **Phase 3**: Comprehensive Testing (JVM) - **COMPLETE**
- ✅ **Phase 4**: Quality Assurance & Tooling - **COMPLETE**
- 🚀 **Phase 5**: Platform Expansion - **IN PROGRESS** (Android Complete)

**Active Phase**: Phase 5 - Platform Expansion (Android ✅ Complete)

**Completed Infrastructure & Architecture**:
- ✅ Kermit logging infrastructure
- ✅ Detekt static analysis integration (passing cleanly)
- ✅ Kover code coverage tracking (>90% line coverage, >85% branch coverage achieved)
- ✅ OWASP dependency-check plugin with NVD API key configured
- ✅ Dokka API documentation generation with end-to-end smoke testing
- ✅ SecureRandomResult<T> sealed class for Result pattern error handling
- ✅ Custom exception hierarchy (6 exception types: SecureRandomException, SecureRandomInitializationException, SecureRandomGenerationException, InvalidParameterException, UnsupportedPlatformException, InsufficientResourcesException)
- ✅ Enhanced SecureRandom interface with Result<T> return types and expanded method set
- ✅ Parameter validation utilities with comprehensive validation functions
- ✅ **JVM platform fully implemented with JvmSecureRandomAdapter**
- ✅ **Android platform fully implemented with AndroidSecureRandomAdapter**
- ✅ **Thread-safe implementation with ReentrantReadWriteLock**
- ✅ **Intelligent algorithm selection (JVM: NativePRNG → Windows-PRNG → SHA1PRNG → Default)**
- ✅ **API level-aware algorithm selection for Android (SHA1PRNG → NativePRNG → Default)**
- ✅ **Comprehensive error handling for JVM and Android-specific failures**
- ✅ All 11 platform implementations updated to match new Result-based API
- ✅ **Comprehensive test suite - Phases 3 & 5 Complete:**
  - ✅ **JVM-specific tests**: 4 advanced test files with statistical randomness, security validation, and performance benchmarking
    - `StatisticalRandomnessTest` - Autocorrelation and monobit frequency tests
    - `SecurityEdgeCaseTest` - Thread safety, resource pressure, temporal independence
    - `PerformanceBenchmarkTest` - Throughput, latency, scalability benchmarks
    - `JvmSecureRandomIntegrationTest` - Real JVM SecureRandom integration validation
  - ✅ **Android-specific tests**: 2 comprehensive test files for Android platform validation
    - `AndroidSecureRandomAdapterTest` - Android adapter functionality, error handling, thread safety (13 test methods)
    - `AndroidSecureRandomIntegrationTest` - Android API level compatibility, algorithm selection, performance testing (10 test methods)
  - ✅ **Cross-platform tests**: 2 advanced common test files for platform validation
    - `StatisticalAdvancedTest` - Chi-square, entropy, distribution validation (cross-platform)
    - `AdvancedEdgeCaseTest` - Boundary values, rapid calls, consistency testing (cross-platform)
  - ✅ **Foundation tests**: 6 original test files (SecureRandomResultTest, SecureRandomExceptionTest, ParameterValidationTest, SecureRandomInterfaceTest, SecureRandomResultAdvancedTest, IntegrationAndEdgeCaseTest)
- ✅ All 20+ KMP targets building successfully
- ✅ **JVM tests fully passing with real implementation**
- ✅ **Android tests fully passing with real implementation**
- ✅ **Statistical validation**: Chi-square, entropy, autocorrelation tests with cross-platform validation
- ✅ **Security testing**: Thread safety, memory security, performance benchmarks
- ✅ Tests running successfully on 12 available platforms (JVM & Android with real impl, others with TODOs)
- ✅ Automated quality gates (qualityGates, enhanced check, quickCheck tasks)
- ✅ Developer-friendly commands and error messages

**Quality Metrics Achieved**:
- 📊 Code Coverage: >90% line coverage, >85% branch coverage
- 🔍 Static Analysis: Zero detekt violations
- 🛡️ Security: OWASP dependency check integrated with NVD API
- 📖 Documentation: Automated API doc generation
- 🧪 Testing: **93+ test methods** across **14 test files**, **JVM & Android tests all passing**
  - **JVM-specific tests**: 4 advanced test files with statistical randomness, security, and performance validation
  - **Android-specific tests**: 2 comprehensive test files with adapter functionality and integration validation
  - **Cross-platform tests**: 2 advanced common test files for platform validation
  - **Foundation tests**: 6 core test files for API and infrastructure validation
- 🚀 Build: All 20+ platforms compiling successfully
- ✅ **JVM Implementation**: Fully functional with java.security.SecureRandom
- ✅ **Android Implementation**: Fully functional with AndroidSecureRandomAdapter and API level awareness
- 🔬 **Phases 3 & 5 Testing Complete**: Statistical randomness, thread safety, performance benchmarks validated

**Next Milestone**:
- **Phase 5**: Platform Expansion - Continue with iOS, JavaScript, Native platforms (Android ✅ Complete)

**Platform Status**:
- **JVM**: ✅ Production-ready implementation
- **Android**: ✅ Production-ready implementation with API level optimization
- **Others**: 🔲 Placeholder TODOs ready for implementation