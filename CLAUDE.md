# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🎯 TL;DR - Project Status

**What Works:**
- **Platform implementations**: 12/12 complete, production-ready, use native crypto APIs
- **Statistical tests**: 14/15 execute successfully, validate randomness quality
- **Security**: Cryptographically secure (wraps platform-native APIs, not custom crypto)

**What Needs Fixing:**
- **NIST compliance**: 73% compliant (3 parameter violations, 2 tests disabled)
- **See compliance checklist below** for specific fixes required

**Critical Rules for AI Agents:**
- ❌ **NEVER** implement custom cryptographic algorithms
- ❌ **NEVER** claim FIPS/NIST certification (we wrap external platforms)
- ✅ **ALWAYS** use platform-native crypto APIs
- ✅ **FIX** NIST parameter violations (see checklist below)

**Priority Tasks:**
1. Fix 3 NIST parameter violations (checklist below)
2. Debug Linear Complexity test (chi-square calculation)
3. Security audit & Maven Central publishing

---

## 📋 NIST SP 800-22 Standards Compliance Checklist

**Compliance Status**: ⚠️ **Partial Compliance** (73% - 11/15 tests standards-compliant)

> 🎯 **Work items to achieve full NIST SP 800-22 compliance:**

### Critical Standards Violations (HIGH Priority)

- [ ] **Frequency within Block Test - Parameter Fix** (`NistSP80022CoreTests.kt:143`)
  - **Issue**: M=128 violates NIST requirement "M > 0.01×n"
  - **Impact**: For QUICK mode (n=100K), requires M > 1000, currently only M=128 (8× too small)
  - **Fix Required**:
    - QUICK mode (100K bits): Increase M from 128 to 1024
    - STANDARD/COMPREHENSIVE (1M bits): Increase M from 128 to 10240 (or 1024 for performance)
    - Update N = n / M calculation accordingly
  - **NIST Reference**: SP 800-22 Section 2.2 - "The recommended value for M is that M > 0.01 × n"
  - **Location**: Line 143: `val M = 128`

- [ ] **Longest Run of Ones Test - Use NIST Table 2-4 Parameters** (`NistSP80022CoreTests.kt:299-313`)
  - **Issue**: Uses custom parameters (n=6272, M=128, N=49) not in NIST Table 2-4
  - **Impact**: Testing with non-validated parameter combinations
  - **Documentation Bug**: Comment says "Uses parameters for n = 75,000" but actually n=6272
  - **Fix Required**: Use official NIST (n, M, N, K) combinations from Table 2-4:
    - Option A: (n=6272, M=8, N=784, K=3) - For short sequences
    - Option B: (n=75000, M=128, N=586, K=5) - For medium sequences
    - Option C: (n=1000000, M=10000, N=100, K=6) - For full 1M bit sequences
  - **NIST Reference**: SP 800-22 Section 2.4, Table 2-4 - Official parameter combinations
  - **Location**: Lines 300-303

- [ ] **DFT Test - Performance vs. Standards Tradeoff** (`NistSP80022AdvancedTests.kt:86-89`)
  - **Issue**: Explicitly caps at 2048 bits due to O(n²) naive DFT (comment: "Cap at 2048 bits for naive DFT performance")
  - **Impact**: STANDARD mode (1M bits) only tests 2048 bits instead of full sequence
  - **Current Code**: `val n = minOf(highestOneBit(NistTestConfig.sequenceLength), 2048)`
  - **Fix Options**:
    - **Option A (Full Compliance)**: Implement FFT algorithm (O(n log n)) for full sequence testing
    - **Option B (Performance)**: Increase cap to 8192-16384 bits, document limitation
    - **Option C (Hybrid)**: Use FFT for STANDARD/COMPREHENSIVE, naive DFT for QUICK mode
  - **NIST Reference**: SP 800-22 Section 2.6 - "It is recommended that each sequence to be tested consist of a minimum of 1000 bits (i.e., n ≥ 1000)"
  - **Note**: Current implementation acknowledges non-compliance in code comment
  - **Location**: Lines 88-89

### Medium Priority Issues

- [ ] **QUICK Mode Sequence Length - Below NIST Minimum** (`NistTestConfig.kt:62`)
  - **Issue**: QUICK mode uses 100K bits, NIST recommends 1M bits minimum
  - **Impact**: Below recommended sequence length for proper validation
  - **Current**: `TestMode.QUICK -> 100_000`
  - **Fix Options**:
    - **Option A**: Increase QUICK to 1M bits (10× slower, full compliance)
    - **Option B**: Rename to "FAST" and document as NIST non-compliant
    - **Option C**: Keep QUICK for CI, recommend STANDARD for certification
  - **NIST Reference**: SP 800-22 Section 4 - "The minimum length of the tested sequence is 1,000,000 bits"
  - **Recommendation**: Option C - Keep for CI performance, document limitations
  - **Location**: Line 62

### Disabled Tests Requiring Debug (HIGH Priority)

- [ ] **Linear Complexity Test - Debug Chi-Square Calculation** (`NistSP80022AdvancedTests.kt:327`)
  - **Status**: Currently disabled with `@Ignore` annotation
  - **Issue**: Chi-square values consistently 77-115 (expected <46.17 for pass)
  - **Root Cause**: "Uncertain - may be Ti normalization, probability distribution, or category boundaries" (comment line 324)
  - **Algorithm Status**: Berlekamp-Massey implementation functional, parameters correct (n=1M, M=500, N=2000)
  - **Debug Steps Required**:
    1. Compare Ti normalization formula: `Ti = (-1)^(M+1) * (L - μ) / σ` against NIST reference
    2. Verify expected value μ and variance σ² formulas for M=500
    3. Check category boundary conditions (7 categories: Ti≤-2.5, -2.5<Ti≤-1.5, ..., Ti>2.5)
    4. Test with NIST reference test vectors (Appendix B test data)
    5. Compare against NIST STS C reference implementation
  - **NIST Reference**: SP 800-22 Section 2.10, Table 2-8 - Linear Complexity probabilities
  - **Resources**: NIST STS reference code at https://csrc.nist.gov/Projects/Random-Bit-Generation/Documentation-and-Software
  - **Location**: Lines 327-417

- [ ] **Maurer's Universal Test - Calibration for Finite K** (`NistSP80022AdvancedTests.kt:469`)
  - **Status**: Currently disabled with `@Ignore` annotation
  - **Issue**: "Produces clustered P-values with shorter sequences" (comment line 466)
  - **Root Cause**: "Needs further calibration for expected value and variance with finite K parameters" (comment line 465)
  - **Minimum Requirements**: NIST requires 387,840 bits for L=6, but QUICK mode only provides 100K
  - **Fix Options**:
    - **Option A**: Verify expected value and variance correction formulas for finite K
    - **Option B**: Disable only for QUICK mode (insufficient bits), enable for STANDARD
    - **Option C**: Implement adaptive L parameter based on available sequence length
  - **NIST Reference**: SP 800-22 Section 2.9 - "For L = 6, Q should be at least 640, and K should be at least 1000"
  - **Current Implementation**: Uses adaptive Q/K based on sequence length, may need formula refinement
  - **Location**: Lines 469-567

### Implementation Guidelines for Future Work

**When Fixing NIST Tests - Follow These Rules**:

1. **Always Consult Official NIST SP 800-22 Rev 1a**:
   - Download from: https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-22r1a.pdf
   - Reference specific section numbers in code comments
   - Use exact formulas and constants from the specification

2. **Parameter Selection Priority**:
   - **First Choice**: Use exact parameters from NIST tables (Tables 2-3, 2-4, 2-5, 2-8, etc.)
   - **Second Choice**: Follow NIST-stated requirements (e.g., "M > 0.01×n", "n ≥ 1,000,000")
   - **Last Resort**: If adapting parameters, document deviation and reasoning in code

3. **Test Configuration Modes**:
   - **QUICK Mode**: Allowed to use smaller sequences for CI performance, but must document as non-compliant
   - **STANDARD Mode**: MUST meet minimum NIST requirements (1M bits, proper parameters)
   - **COMPREHENSIVE Mode**: Should exceed minimum requirements for high-confidence validation

4. **Validation Against Reference Implementation**:
   - When debugging failing tests, compare against NIST STS C reference implementation
   - Use NIST test vectors from Appendix B for known-good data validation
   - Cross-check mathematical formulas line-by-line with specification

5. **Documentation Requirements**:
   - Add NIST section references in comments: `// NIST SP 800-22 Section 2.X`
   - Document any deviations with clear justification: `// NOTE: Using M=1024 instead of M=10240 for performance`
   - Update compliance status in CLAUDE.md when fixing issues

6. **Multi-Sequence Testing Requirements** (NIST Section 4.2):
   - Minimum 55 sequences recommended, 100+ sequences for production
   - Must check BOTH proportion passing AND P-value uniformity
   - Proportion must fall within confidence interval: `p̂ ± 3√(p̂(1-p̂)/m)` where p̂=0.99, m=sequence count
   - P-value uniformity: Chi-square test across 10 bins, minimum P-value ≥ 0.0001

**Reference Resources**:
- Official Spec: https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-22r1a.pdf
- NIST STS Software: https://csrc.nist.gov/Projects/Random-Bit-Generation/Documentation-and-Software
- Test Vectors: NIST STS Appendix B (use for validation)
- Reference Implementation: https://github.com/terrillmoore/NIST-Statistical-Test-Suite (unofficial mirror)

**Compliance Summary Table**:

| Standard | Tests | Fully Compliant | Has Issues | Disabled | Compliance Rate |
|----------|-------|-----------------|------------|----------|-----------------|
| **FIPS 140-2** | 5 | 5 | 0 | 0 | **100%** ✅ |
| **NIST Core** | 5 | 3 | 2 | 0 | **60%** ⚠️ |
| **NIST Advanced** | 5 | 3 | 0 | 2 | **60%** ⚠️ |
| **Overall** | 15 | 11 | 2 | 2 | **73%** |

**Fully Compliant Tests** (No Changes Required):
- ✅ FIPS 140-2: Monobit, Poker, Runs, Long Run (all use exact FIPS parameters)
- ✅ NIST Runs Test (correct prerequisite check, proper formulas)
- ✅ NIST Binary Matrix Rank Test (M=Q=32, correct probabilities)
- ✅ NIST Cumulative Sums Test (both forward/backward modes, correct P-value calculation)
- ✅ NIST Approximate Entropy Test (m=2, proper φ(m) calculation)
- ✅ NIST Serial Test (m=3, both ∇ψ²m and ∇²ψ²m statistics)

---

## 🔨 Build Commands

**Repository**: https://github.com/scottnj/KMP-Secure-Random

### Essential Commands
```shell
./gradlew build                    # Build all platforms
./gradlew allTests                 # Run all tests
./gradlew qualityGates            # Full quality validation
./gradlew check                   # Enhanced checks with quality gates
./gradlew quickCheck              # Fast local dev checks (JVM tests, coverage, detekt)
```

### Statistical Testing
```shell
./gradlew fipsTests               # FIPS 140-2 compliance tests
./gradlew nistCoreTests           # NIST SP 800-22 Core tests (5 tests)
./gradlew nistAdvancedTests       # NIST SP 800-22 Advanced tests (5 tests, 1 disabled)
./gradlew nistTests               # All NIST tests (core + advanced)
./gradlew complianceReport        # Comprehensive compliance report
```

### Platform-Specific Testing
```shell
./gradlew jvmTest                 # JVM
./gradlew testDebugUnitTest       # Android
./gradlew iosSimulatorArm64Test   # iOS Simulator
./gradlew macosArm64Test          # macOS
./gradlew tvosSimulatorArm64Test  # tvOS Simulator
./gradlew watchosSimulatorArm64Test # watchOS Simulator
./gradlew jsTest                  # JavaScript
./gradlew wasmJsTest              # WASM-JS
./gradlew linuxX64Test            # Linux x64
./gradlew mingwX64Test            # Windows (MinGW)
```

### Quality Tools
```shell
./gradlew detekt                  # Static analysis
./gradlew koverHtmlReport         # Coverage report
./gradlew koverVerify             # Verify >90% line, >85% branch coverage
./gradlew dependencyCheckAnalyze  # OWASP security scan
./gradlew dokkaHtml               # API documentation
```

---

## 🏗️ Architecture & Security Principles

### Project Structure

```
/shared - Main module containing secure random library
├── src/commonMain/kotlin - Platform-agnostic implementation
├── src/*/kotlin - Platform-specific implementations (expect/actual)
└── src/commonTest/kotlin - Cross-platform tests
```

### Core Design Rules

**Security-First (CRITICAL)**:
- ❌ **NEVER implement custom cryptographic algorithms**
- ✅ **ALWAYS use platform-native crypto APIs**:
  - JVM: `java.security.SecureRandom`
  - Apple: `SecRandomCopyBytes` (Security.framework) or `arc4random()`
  - JavaScript: Web Crypto API / Node.js crypto
  - Linux: `getrandom()` syscall + `/dev/urandom` fallback
  - Windows: `BCryptGenRandom` or `CryptGenRandom`
- ✅ Result-based error handling (`SecureRandomResult<T>`)
- ✅ Secure-by-default with explicit `@AllowInsecureFallback` opt-in
- ✅ Thread-safe implementations guaranteed

**Platform Strategy**:
- Adapter pattern for each platform
- `expect/actual` for KMP compatibility
- Per-architecture source sets where needed (watchOS, Android Native)

### Critical Platform Implementation Notes

**Android Native** - Requires Per-Architecture Source Sets:
- **Problem**: Different syscall numbers and bit widths across architectures
- **Solution**: Separate source sets for ARM32, ARM64, x86, x86_64
  - `androidNativeArm32Main` - ARM32 syscall #384, 32-bit UInt types
  - `androidNativeArm64Main` - ARM64 syscall #278, 64-bit ULong types
  - `androidNativeX86Main` - x86 syscall #355, 32-bit UInt types
  - `androidNativeX64Main` - x86_64 syscall #318, 64-bit ULong types
- **Pattern**: `getrandom()` syscall + `/dev/urandom` fallback
- **Status**: ✅ Complete, GitHub Actions validated

**watchOS** - Isolated from Apple Platforms:
- **Problem**: Bit width conflicts in KMP metadata compilation
- **Solution**: Isolated `watchosMain` source set using `arc4random()`
- **Why Different**: Other Apple platforms use `SecRandomCopyBytes`
- **Status**: ✅ Complete, architectural separation working

**WASM-JS** - Secure-by-Default with Fallback:
- **Browser**: Web Crypto API (`crypto.getRandomValues()`)
- **D8 Environment**: Fails with `SecureRandomInitializationException` (no Web Crypto)
- **Fallback**: Math.random only with explicit `@OptIn(AllowInsecureFallback)`
- **Status**: ✅ Complete, security warnings in place

**Windows MinGW** - BCryptGenRandom Limitation:
- **Primary**: `BCryptGenRandom` (modern CNG API)
- **Fallback**: `CryptGenRandom` (legacy Crypto API)
- **Issue**: MinGW lacks bcrypt.dll import libraries for static linking
- **Security Impact**: NONE - Both APIs are FIPS 140-2 validated and cryptographically equivalent
- **Status**: ✅ Known limitation, documented, no security concern

### Security Framework

**Fallback Policy System**:

```kotlin
// Secure by Default (Recommended)
val secureRandom = createSecureRandom().getOrThrow()

// Explicit Insecure Fallback (Use with Caution)
@OptIn(AllowInsecureFallback::class)
val secureRandom = createSecureRandom(FallbackPolicy.ALLOW_INSECURE).getOrThrow()
```

**Platform Security Characteristics**:

| Platform | Primary Method | Secure Fallback | Insecure Fallback |
|----------|---------------|-----------------|-------------------|
| **JVM** | `SecureRandom` (various algorithms) | ✅ Multiple secure providers | ❌ None |
| **Android** | `SHA1PRNG`/`NativePRNG` | ✅ Secure provider chain | ❌ None |
| **iOS/macOS/tvOS** | `SecRandomCopyBytes` | ✅ None needed | ❌ None |
| **watchOS** | `arc4random` | ✅ None needed | ❌ None |
| **JavaScript** | Web Crypto API / Node.js crypto | ✅ None needed | ❌ None |
| **WASM-JS** | Web Crypto API | ❌ None available | ⚠️ Math.random (opt-in only) |
| **Linux/Android Native** | `getrandom()` syscall | ✅ `/dev/urandom` | ❌ None |
| **Windows** | `BCryptGenRandom` (CNG API) | ✅ `CryptGenRandom` (legacy) | ❌ None |

**Policy Details**:
- `FallbackPolicy.SECURE_ONLY` (Default): Only cryptographically secure methods allowed
- `FallbackPolicy.ALLOW_INSECURE`: Permits insecure fallbacks with explicit `@OptIn(AllowInsecureFallback)`
- Compiler warnings via `@RequiresOptIn` ensure developers understand security implications
- Only WASM-JS platform has insecure fallbacks; others ignore policy safely

---

## 📊 Platform Implementation Status

**Overall**: 12/12 platforms complete, production-ready

| Platform | Status | Implementation | Security Level | Notes |
|----------|--------|----------------|----------------|-------|
| **JVM** | ✅ Production | `java.security.SecureRandom` | Cryptographically secure | Rejection sampling for modulo bias |
| **Android** | ✅ Production | `SHA1PRNG`/`NativePRNG` | Cryptographically secure | API-level optimization |
| **iOS** | ✅ Production | `SecRandomCopyBytes` | Cryptographically secure | Security.framework |
| **macOS** | ✅ Production | `SecRandomCopyBytes` | Cryptographically secure | Security.framework |
| **tvOS** | ✅ Production | `SecRandomCopyBytes` | Cryptographically secure | Security.framework |
| **watchOS** | ✅ Production | `arc4random()` | Cryptographically secure | Isolated source set |
| **JavaScript** | ✅ Production | Web Crypto/Node.js crypto | Cryptographically secure | Auto-detection |
| **WASM-JS** | ✅ Production | Web Crypto API | Cryptographically secure | Math.random opt-in fallback |
| **Linux** | ✅ Production | `getrandom()` syscall | Cryptographically secure | `/dev/urandom` fallback, GitHub Actions validated |
| **Windows** | ✅ Production | `CryptGenRandom` | Cryptographically secure | MinGW limitation (BCryptGenRandom unavailable), GitHub Actions validated |
| **Android Native ARM32** | ✅ Production | `getrandom()` syscall #384 | Cryptographically secure | Per-architecture source set, GitHub Actions validated |
| **Android Native ARM64** | ✅ Production | `getrandom()` syscall #278 | Cryptographically secure | Per-architecture source set, GitHub Actions validated |
| **Android Native x86** | ✅ Production | `getrandom()` syscall #355 | Cryptographically secure | Per-architecture source set, GitHub Actions validated |
| **Android Native x86_64** | ✅ Production | `getrandom()` syscall #318 | Cryptographically secure | Per-architecture source set, GitHub Actions validated |

---

## 🧪 Statistical Testing Summary

**Test Execution Success**: 14/15 tests run successfully
**Standards Compliance**: 11/15 tests (73%) use exact specification parameters
**Quality Validation**: All executing tests confirm good randomness

### Test Suites Implemented

**FIPS 140-2 Statistical Tests** (5/5 - 100% Compliant ✅):
1. Monobit Test - Equal distribution of 0s and 1s (9,726-10,274 ones in 20K bits)
2. Poker Test - 4-bit pattern uniformity (X statistic: 2.16-46.17)
3. Runs Test - Validates run lengths 1-6+ for both 0s and 1s (strict criteria)
4. Long Run Test - Ensures no runs ≥26 bits (critical failure detector)
5. Full Compliance Test - Comprehensive validation report

**NIST SP 800-22 Core Tests** (5/5 Executing, 3/5 Compliant):
1. Frequency Test within a Block ⚠️ (M parameter deviation - see checklist)
2. Runs Test ✅ (fully compliant)
3. Longest Run of Ones Test ⚠️ (non-standard parameters - see checklist)
4. Binary Matrix Rank Test ✅ (fully compliant)
5. Cumulative Sums (Cusum) Test ✅ (fully compliant)

**NIST SP 800-22 Advanced Tests** (4/5 Executing, 3/5 Compliant):
1. Discrete Fourier Transform (Spectral) Test ⚠️ (capped at 2048 bits - see checklist)
2. Approximate Entropy Test ✅ (fully compliant)
3. Serial Test ✅ (fully compliant)
4. Linear Complexity Test ❌ (disabled - needs calibration, see checklist)
5. Maurer's Universal Statistical Test ✅ (fully compliant)

**Test Configuration**:
- Significance level: α = 0.01 (99% confidence)
- Multi-iteration approach: 5 iterations per test with majority voting
- Robust validation: Requires 3/5 passes to reduce false positives
- Cross-platform: All tests run on all 12 KMP targets

**Important Notes**:
- ✅ All executing tests validate randomness quality
- ⚠️ Some NIST tests have parameter deviations (see compliance checklist above)
- ❌ This library cannot obtain FIPS 140-2 certification (wraps external platform implementations)

**For Full Details**: See [STATISTICAL_TESTING_SUMMARY.md](./STATISTICAL_TESTING_SUMMARY.md)

---

## 📚 Reference Resources

**Official Standards**:
- NIST SP 800-22 Rev 1a: https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-22r1a.pdf
- NIST STS Software: https://csrc.nist.gov/Projects/Random-Bit-Generation/Documentation-and-Software
- FIPS 140-2: https://csrc.nist.gov/publications/detail/fips/140/2/final

**Implementation Reference**:
- NIST STS C code (unofficial): https://github.com/terrillmoore/NIST-Statistical-Test-Suite
- Statistical test details: [STATISTICAL_TESTING_SUMMARY.md](./STATISTICAL_TESTING_SUMMARY.md)
- Project README: [README.md](./README.md)
- Reorganization plan: [CLAUDE_REORGANIZATION_PLAN.md](./CLAUDE_REORGANIZATION_PLAN.md)

**Development Resources**:
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html
- GitHub Actions CI/CD: https://github.com/scottnj/KMP-Secure-Random/actions

---

## 📋 Implementation Status & Roadmap

**Current Status**: 12/12 platforms complete, 73% NIST standards-compliant

**Completed**:
- [x] All 12 platform families implemented (JVM, Android, Apple platforms, JS/WASM, Linux, Windows, Android Native)
- [x] FIPS 140-2 statistical tests (100% compliant)
- [x] NIST SP 800-22 test suite (73% compliant - 3 parameter violations, 2 disabled)
- [x] CI/CD pipeline and documentation
- [x] Comprehensive security framework with fallback policies
- [x] Cross-platform test suite with ~30 focused test files
- [x] Quality gates: Detekt, Kover, OWASP, Dokka

**Remaining Work**:
- [ ] Fix 3 NIST parameter violations (see compliance checklist above)
- [ ] Debug Linear Complexity test (chi-square calculation)
- [ ] Calibrate Maurer's Universal test (finite K formulas)
- [ ] Security audit and penetration testing
- [ ] Maven Central publishing setup
- [ ] Optional: NIST SP 800-22 Template Tests (4 tests - deferred for future enhancement)
- [ ] Optional: Randomness quality monitoring infrastructure (deferred)

**Priority Order**:
1. **NIST Compliance Fixes** (see checklist above) - High priority
2. **Disabled Tests Debug** - High priority
3. **Security Audit** - Before 1.0 release
4. **Maven Central Publishing** - For public release
