# GitHub Actions CI/CD Pipeline

This directory contains automated testing and validation workflows for the KMP Secure Random library.

## Workflows Overview

### 🔄 `ci.yml` - Comprehensive CI/CD Pipeline
**Trigger**: Push to `main`/`develop`, Pull Requests to `main`

A comprehensive pipeline that validates the entire codebase across all supported platforms:

- **Quality Gates**: Static analysis, code coverage, security scanning
- **Linux Tests**: Native Linux testing on Ubuntu runners
- **Cross-Platform Build**: Verification that all 20+ KMP targets compile
- **JVM Tests**: Reference implementation testing across Java 11/17/21
- **JavaScript Tests**: Node.js and browser testing across multiple versions
- **Android Tests**: Android unit testing
- **Security Scan**: OWASP dependency vulnerability analysis

### 🐧 `linux-tests.yml` - Linux Platform Focus
**Trigger**: Changes to Linux implementation, common code, or tests

Specialized pipeline for Linux platform validation:

- **Compilation Check**: Verifies Linux x64 and ARM64 compilation
- **Native Testing**: Runs Linux x64 tests on multiple Ubuntu versions
- **Security Validation**: Linux-specific security analysis
- **Performance Benchmark**: Performance validation for Linux implementation

## Linux Testing Capabilities

### ✅ What GitHub Actions CAN Test

**Linux X64 (Fully Supported)**:
- ✅ Native Linux test execution on `ubuntu-latest`, `ubuntu-22.04`, `ubuntu-24.04`
- ✅ Real `getrandom()` syscall testing on Linux 3.17+ kernels
- ✅ Actual `/dev/urandom` fallback validation
- ✅ Linux-specific error condition testing (EAGAIN, EINTR, ENOSYS)
- ✅ Statistical randomness validation using Linux kernel entropy
- ✅ Thread safety and concurrent access testing
- ✅ Memory safety validation with cinterop

**Linux ARM64 (Compilation Only)**:
- ✅ Cross-compilation verification for ARM64
- ✅ Test executable linking for ARM64
- ❌ Native ARM64 test execution (requires GitHub Enterprise or self-hosted runners)

### 🧪 Test Coverage

**Comprehensive Test Suite (13 Test Files)**:
1. **BasicSecureRandomTest** - Core API validation (15 tests)
2. **StatisticalBasicTest** - Cross-platform randomness validation (7 tests)
3. **EdgeCaseTest** - Parameter validation and edge cases (9 tests)
4. **StatisticalAdvancedTest** - Advanced statistical validation
5. **AdvancedEdgeCaseTest** - Boundary condition testing
6. **SecureRandomResultTest** - Result<T> pattern validation
7. **SecureRandomExceptionTest** - Exception hierarchy testing
8. **ParameterValidationTest** - Parameter validation logic
9. **SecureRandomInterfaceTest** - Interface contract validation
10. **IntegrationAndEdgeCaseTest** - Integration scenarios
11. **SecureRandomResultAdvancedTest** - Advanced Result<T> testing
12. **SmokeTest** - Basic platform functionality
13. **SharedCommonTest** - Cross-platform compatibility

**Real Linux Validation**:
- ✅ Tests run on actual Linux machines (GitHub's Ubuntu runners)
- ✅ Real Linux kernel entropy sources (`/dev/urandom`, `getrandom()`)
- ✅ Actual Linux syscall validation and error handling
- ✅ Statistical quality validation using Linux kernel randomness
- ✅ Performance benchmarking on Linux hardware

## Running Workflows

### Automatic Triggers
- **Push to main/develop**: Runs full CI/CD pipeline
- **Pull Request to main**: Runs full validation
- **Linux code changes**: Triggers focused Linux testing

### Manual Triggers
```bash
# Trigger via GitHub web interface:
# 1. Go to "Actions" tab in GitHub repository
# 2. Select workflow
# 3. Click "Run workflow"

# Or via GitHub CLI:
gh workflow run ci.yml
gh workflow run linux-tests.yml
```

### Local Testing (Before Push)
```bash
# Verify Linux implementation compiles
./gradlew compileKotlinLinuxX64 compileKotlinLinuxArm64

# Verify tests compile
./gradlew compileTestKotlinLinuxX64 compileTestKotlinLinuxArm64

# Run quality gates
./gradlew qualityGates

# Run available tests (JVM as proxy for Linux validation)
./gradlew jvmTest
```

## Security Configuration

### Required Secrets
Add these secrets in GitHub repository settings:

```
NVD_API_KEY=your_nvd_api_key_here
```

**How to get NVD API Key**:
1. Visit [NVD API](https://nvd.nist.gov/developers/request-an-api-key)
2. Request API key for vulnerability scanning
3. Add to GitHub Secrets: Settings → Secrets and variables → Actions → New repository secret

### Security Features
- **OWASP Dependency Check**: Automated vulnerability scanning
- **Static Code Analysis**: Detekt security rules
- **Code Coverage**: 90% line coverage requirement
- **Security Implementation Validation**: Verifies use of secure APIs only

## Artifacts and Reports

### Generated Artifacts
- **Test Results**: JUnit XML reports for all platforms
- **Code Coverage**: HTML and XML coverage reports
- **Security Reports**: OWASP dependency check reports
- **Build Binaries**: Cross-platform test executables
- **Quality Reports**: Detekt static analysis results

### Artifact Retention
- **Test Results**: 30 days
- **Security Reports**: 30 days
- **Build Binaries**: 7 days

## Understanding Results

### ✅ Success Indicators
```
✅ All CI checks passed!
🐧 Linux platform implementation: PASSED
✅ getrandom() syscall implementation validated
✅ /dev/urandom fallback implementation validated
✅ Cross-platform test suite passed on Linux
```

### ❌ Failure Indicators
- Compilation errors in Linux targets
- Test failures in Linux execution
- Security vulnerabilities detected
- Code coverage below 90%
- Static analysis violations

### 📊 Test Metrics
- **Execution Time**: Linux tests typically complete in 5-10 minutes
- **Test Count**: 80+ test methods across all test files
- **Coverage**: Validates both x64 and ARM64 architectures
- **Environments**: Tests across multiple Ubuntu versions

## ARM64 Linux Testing

Currently ARM64 Linux testing is limited to compilation verification due to GitHub Actions limitations:

**Available**:
- ✅ ARM64 cross-compilation validation
- ✅ Test executable linking
- ✅ Static analysis

**Requires Self-Hosted Runners**:
- ❌ Native ARM64 test execution
- ❌ ARM64-specific performance benchmarks

**To Enable ARM64 Testing**:
1. Set up self-hosted ARM64 Linux runners
2. Add runner labels to workflow matrix
3. Configure runner-specific steps

## Troubleshooting

### Common Issues

**Linux Tests Skipped**:
- Check that code changes trigger the workflow
- Verify runner availability
- Check workflow syntax

**Permission Errors**:
- Ensure NVD_API_KEY secret is configured
- Check repository permissions for Actions

**Build Failures**:
- Verify Gradle wrapper is executable
- Check Java/Node.js version compatibility
- Review dependency conflicts

### Getting Help

1. **Check workflow logs**: Click on failed job in Actions tab
2. **Review artifacts**: Download test reports and logs
3. **Local reproduction**: Run same commands locally
4. **Gradle debugging**: Add `--debug` flag for verbose output

## Future Enhancements

**Planned Improvements**:
- [ ] ARM64 native testing with self-hosted runners
- [ ] Performance regression detection
- [ ] Automated security baseline updates
- [ ] Integration with external security scanners
- [ ] Automated release pipeline

**Platform Expansion**:
- [ ] Windows native testing
- [ ] macOS native testing (Apple Silicon and Intel)
- [ ] Additional Linux distributions (CentOS, Alpine)
- [ ] Container-based testing