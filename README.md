# KMP Secure Random

[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?logo=opensourceinitiative)](https://opensource.org/licenses/MIT)
[![Security](https://img.shields.io/badge/security-native%20APIs%20only-green.svg?logo=shield)](https://github.com/scottnj/KMP-Secure-Random#important-we-dont-roll-our-own-crypto)

[![JVM/Android](https://img.shields.io/badge/JVM%2FAndroid-JVM%20%7C%20Android-brightgreen.svg?logo=android)](https://kotlinlang.org/docs/multiplatform.html)
[![Apple](https://img.shields.io/badge/Apple-iOS%20%7C%20macOS%20%7C%20watchOS%20%7C%20tvOS-brightgreen.svg?logo=apple)](https://kotlinlang.org/docs/multiplatform.html)
[![Web](https://img.shields.io/badge/Web-JavaScript%20%7C%20WASM-brightgreen.svg?logo=javascript)](https://kotlinlang.org/docs/multiplatform.html)
[![Native](https://img.shields.io/badge/Native-Linux%20%7C%20Windows%20%7C%20MinGW-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg?logo=github)](https://github.com/scottnj/KMP-Secure-Random/actions)
[![Quality Gates](https://img.shields.io/badge/quality%20gates-passing-brightgreen.svg?logo=checkmarx)](https://github.com/scottnj/KMP-Secure-Random)
[![Static Analysis](https://img.shields.io/badge/detekt-passing-brightgreen.svg?logo=sonarqube)](https://github.com/scottnj/KMP-Secure-Random)
[![Coverage](https://img.shields.io/badge/coverage-~80%25-yellow.svg)](https://github.com/scottnj/KMP-Secure-Random)

A Kotlin Multiplatform library for secure random number generation across all supported platforms.

**Repository**: https://github.com/scottnj/KMP-Secure-Random

## Overview

This library provides cryptographically secure random number generation for Kotlin Multiplatform projects, targeting **all possible KMP platforms** including JVM, Android, iOS, macOS, watchOS, tvOS, JavaScript, WASM-JS, Linux, Windows, and Android Native variants.

### ⚠️ **Important: We Don't Roll Our Own Crypto**

This library **does NOT implement custom cryptographic algorithms**. Instead, it provides a unified KMP interface that wraps each platform's **native, battle-tested cryptographic implementations**:

- **JVM**: Uses `java.security.SecureRandom`
- **Android**: Uses `java.security.SecureRandom` with Android-specific optimizations
- **Apple Platforms**: Uses Apple's `SecRandomCopyBytes` and `arc4random` system APIs
- **JavaScript**: Uses Web Crypto API (`crypto.getRandomValues()`) and Node.js crypto (`crypto.randomBytes()`)
- **WASM-JS**: Uses Web Crypto API in browsers, with statistical fallback for testing environments
- **Linux**: Uses `getrandom()` syscall (Linux 3.17+) with `/dev/urandom` fallback

**Goal**: Make existing, proven secure random implementations easily accessible across all KMP platforms with a consistent, type-safe API.

## Platform Support

| Platform | Status | Implementation | Security Level |
|----------|--------|----------------|----------------|
| JVM | ✅ **Production Ready** | `java.security.SecureRandom` | Cryptographically secure |
| Android | ✅ **Production Ready** | `AndroidSecureRandomAdapter` with API-level optimization | Cryptographically secure |
| iOS | ✅ **Production Ready** | `AppleSecureRandomAdapter` using `SecRandomCopyBytes` | Cryptographically secure |
| macOS | ✅ **Production Ready** | `AppleSecureRandomAdapter` using `SecRandomCopyBytes` | Cryptographically secure |
| tvOS | ✅ **Production Ready** | `AppleSecureRandomAdapter` using `SecRandomCopyBytes` | Cryptographically secure |
| watchOS | ✅ **Production Ready** | `WatchosSecureRandomAdapter` using `arc4random` | Cryptographically secure |
| JavaScript | ✅ **Production Ready** | `JsSecureRandomAdapter` with Web Crypto API/Node.js crypto | Cryptographically secure |
| WASM-JS (Browser) | ✅ **Production Ready** | `WasmJsSecureRandomAdapter` using Web Crypto API | Cryptographically secure |
| WASM-JS (D8) | ⚠️ **Testing Only** | `WasmJsSecureRandomAdapter` using Math.random fallback | Statistical quality only |
| Linux x64 | ✅ **Production Ready** | `LinuxSecureRandomAdapter` using `getrandom()` + `/dev/urandom` fallback | Cryptographically secure |
| Linux ARM64 | ✅ **Production Ready** | `LinuxSecureRandomAdapter` using `getrandom()` + `/dev/urandom` fallback | Cryptographically secure |
| Windows | 🔲 **Planned** | `BCryptGenRandom` with `CryptGenRandom` fallback | Cryptographically secure |
| MinGW | 🔲 **Planned** | Windows API compatibility | Cryptographically secure |
| Android Native (x64/x86/arm32/arm64) | 🔲 **Planned** | Direct NDK random API access | Cryptographically secure |

### WASM-JS Environment Notes

The WASM-JS implementation uses intelligent environment detection:

- **Browser Environment**: Uses Web Crypto API (`crypto.getRandomValues()`) for cryptographically secure randomness
- **D8 Environment**: Uses enhanced Math.random fallback with XOR of multiple sources for improved statistical properties

**About D8**: D8 is Google V8's command-line JavaScript shell used for testing. It lacks Web APIs including the Web Crypto API, so our implementation provides a statistically robust fallback using `(r1 ^ r2 ^ r3 ^ r4) & 0xFF` to pass statistical quality tests while clearly marking it as not cryptographically secure.

## Features

- **Result-based Error Handling**: All operations return `SecureRandomResult<T>` instead of throwing exceptions
- **Thread-Safe**: All implementations are guaranteed thread-safe
- **Cross-Platform API**: Consistent interface across all platforms
- **Statistical Quality**: Passes rigorous statistical tests (chi-square, monobit frequency, entropy analysis)
- **Production Ready**: Comprehensive test suite with 314+ tests across all platforms

## Usage

```kotlin
// Create a secure random instance
val secureRandom = createSecureRandom().getOrThrow()

// Generate random bytes
val bytes = ByteArray(32)
secureRandom.nextBytes(bytes).getOrThrow()

// Generate random integers
val randomInt = secureRandom.nextInt(100).getOrThrow() // 0-99
val rangedInt = secureRandom.nextInt(10, 20).getOrThrow() // 10-19

// Generate other types
val randomLong = secureRandom.nextLong().getOrThrow()
val randomBoolean = secureRandom.nextBoolean().getOrThrow()
val randomDouble = secureRandom.nextDouble().getOrThrow() // 0.0-1.0
val randomFloat = secureRandom.nextFloat().getOrThrow() // 0.0f-1.0f

// Generate byte arrays of specific sizes
val randomBytes = secureRandom.nextBytes(256).getOrThrow()
```

## Error Handling

The library uses a Result-based approach for comprehensive error handling:

```kotlin
val result = secureRandom.nextBytes(1024)
when {
    result.isSuccess -> {
        val bytes = result.getOrNull()!!
        // Use the random bytes
    }
    result.isFailure -> {
        val exception = result.exceptionOrNull()
        when (exception) {
            is SecureRandomGenerationException -> { /* Handle generation failure */ }
            is InsufficientResourcesException -> { /* Handle memory issues */ }
            is InvalidParameterException -> { /* Handle invalid parameters */ }
        }
    }
}
```

## Build Commands

### Build the library
```shell
./gradlew build
```

### Run tests
```shell
./gradlew allTests              # All platforms
./gradlew jvmTest              # JVM only
./gradlew wasmJsTest           # WASM-JS only
./gradlew iosSimulatorArm64Test # iOS only
./gradlew linuxX64Test         # Linux x64 (requires Linux machine)
```

### GitHub Actions CI/CD
```shell
# Automated testing on every push/PR:
# - Linux tests run on real Ubuntu machines (ubuntu-latest, 22.04, 24.04)
# - Cross-platform compilation verification for all 20+ targets
# - Quality gates with static analysis, coverage, and security scanning
# - Performance benchmarks and statistical randomness validation

# View test results: https://github.com/scottnj/KMP-Secure-Random/actions
```

### Quality checks
```shell
./gradlew qualityGates  # Full quality validation
./gradlew check        # Enhanced checks
./gradlew quickCheck   # Fast local development checks
```

## Architecture

The library follows clean architecture principles:

- **Domain Layer**: Core `SecureRandom` interface with `Result<T>` error handling
- **Infrastructure Layer**: Platform-specific adapters using the adapter pattern
- **Clean Separation**: Each platform implementation is isolated and testable

### Security Features

- **Cryptographic Quality**: Uses platform-native secure random APIs
- **Memory Security**: Secure handling and clearing of sensitive data
- **Thread Safety**: All implementations use appropriate synchronization
- **Error Resilience**: Comprehensive error handling with graceful degradation

## Testing

The library includes comprehensive testing with automated CI/CD:

### Test Infrastructure
- **28 test files** with 314+ test methods
- **Statistical validation** (chi-square, entropy, autocorrelation tests)
- **Security testing** (thread safety, memory security, performance benchmarks)
- **Cross-platform compatibility** testing
- **100% test pass rate** across all implemented platforms

### GitHub Actions CI/CD
- **Linux Testing**: Real tests on Ubuntu runners (latest, 22.04, 24.04)
- **Cross-Platform Build**: All 20+ KMP targets verified
- **Quality Gates**: Static analysis, coverage (90%+ target), security scanning
- **Automated Validation**: Every push/PR triggers comprehensive testing
- **Platform-Specific Validation**: Tests run with actual platform APIs (getrandom(), Web Crypto API, etc.)

## Dependencies

- **Kermit**: Cross-platform logging
- **Kotlin Test**: Testing framework
- **Platform-Native Crypto APIs**:
  - **Zero additional crypto dependencies** - uses only built-in platform APIs
  - **JVM**: `java.security.SecureRandom` (built into JDK)
  - **Apple**: `Security` framework APIs (built into iOS/macOS/etc.)
  - **JavaScript**: Web Crypto API / Node.js crypto (built into runtimes)
  - **Android**: Android system crypto APIs (built into Android)
  - **Linux**: Linux kernel APIs (`getrandom()` syscall, `/dev/urandom`)

This approach ensures maximum security, minimal attack surface, and leverages decades of cryptographic engineering by platform vendors.

## License

MIT License

Copyright (c) 2025 Scott Whitman

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Contributing

We welcome contributions to KMP Secure Random! This project aims to provide secure, reliable random number generation across all Kotlin Multiplatform targets.

### 🛡️ **Security-First Development**

**IMPORTANT**: This library wraps platform-native cryptographic APIs. We do **NOT** implement custom cryptographic algorithms.

- ✅ **DO**: Improve existing platform adapters, add new platform support, enhance error handling
- ✅ **DO**: Add tests, improve documentation, fix bugs in our wrapper logic
- ❌ **DON'T**: Implement custom random number generators or cryptographic algorithms
- ❌ **DON'T**: Replace platform-native crypto APIs with custom implementations

### 🚀 **Getting Started**

1. **Fork and Clone**
   ```bash
   git clone https://github.com/[your-username]/KMP-Secure-Random.git
   cd KMP-Secure-Random
   ```

2. **Set up Development Environment**
   - Install JDK 11+
   - Install Android SDK (for Android targets)
   - Install Xcode (for Apple platforms, macOS only)
   - Install Node.js (for JavaScript/WASM targets)

3. **Run Tests**
   ```bash
   ./gradlew jvmTest                # Test JVM implementation
   ./gradlew testDebugUnitTest      # Test Android implementation
   ./gradlew iosSimulatorArm64Test  # Test iOS implementation
   ./gradlew jsTest                 # Test JavaScript implementation
   ./gradlew wasmJsTest            # Test WASM-JS implementation
   ```

4. **Run Quality Checks**
   ```bash
   ./gradlew quickCheck    # Fast local checks (recommended for development)
   ./gradlew check         # Full quality validation
   ./gradlew detekt        # Static analysis only
   ```

### 🎯 **Contribution Areas**

#### **High Priority**
- **Native Platform Support**: Implement Linux, Windows, Android Native platforms
- **Test Coverage**: Improve statistical and security test coverage
- **Documentation**: API docs, usage examples, platform-specific notes

#### **Platform Implementation Guidelines**
When adding a new platform:

1. **Use Platform-Native APIs**: Always wrap the platform's built-in secure random API
2. **Follow Adapter Pattern**: Create a `[Platform]SecureRandomAdapter` class
3. **Comprehensive Testing**: Add both unit tests and integration tests
4. **Error Handling**: Use `SecureRandomResult<T>` for all operations
5. **Thread Safety**: Ensure all implementations are thread-safe
6. **Documentation**: Update README platform support table

#### **Example Platform Implementation**
```kotlin
// LinuxSecureRandomAdapter.kt
class LinuxSecureRandomAdapter : SecureRandomAdapter {
    override fun fillBytesInternal(bytes: ByteArray) {
        // Use Linux's /dev/urandom or getrandom() syscall
        // NO custom crypto algorithms!
    }
}
```

### 📋 **Pull Request Process**

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/linux-platform-support
   ```

2. **Make Changes**
   - Follow existing code patterns
   - Add comprehensive tests
   - Update documentation

3. **Validate Changes**
   ```bash
   ./gradlew quickCheck  # Must pass
   ```

4. **Submit PR**
   - Clear title describing the change
   - Reference any related issues
   - Include test results for new platforms

### 🧪 **Testing Requirements**

All contributions must include tests:

- **Unit Tests**: Test your adapter's functionality
- **Integration Tests**: Test real platform API integration
- **Statistical Tests**: Ensure randomness quality (for new platforms)
- **Cross-Platform Tests**: Verify common interface compliance

### 📝 **Code Style**

- Follow existing Kotlin conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep security-related code simple and auditable

### 🐛 **Reporting Issues**

When reporting security-related issues:

- **Verify the issue is in our wrapper code**, not the underlying platform crypto API
- Include platform details (OS version, Kotlin version, etc.)
- Provide minimal reproduction case
- For security vulnerabilities, consider private disclosure first

### 📚 **Resources**

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Platform Crypto APIs Documentation](./docs/platform-apis.md) *(if you create this)*
- [Project Architecture](./CLAUDE.md) - Detailed implementation status

### ❓ **Questions?**

- Open a discussion for architecture questions
- Check existing issues for similar problems
- Read through `CLAUDE.md` for project context

Thank you for contributing to secure, cross-platform random number generation! 🔒