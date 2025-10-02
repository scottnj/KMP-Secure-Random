// Custom Karma configuration for Chrome browser testing
// This file is automatically merged into the generated karma.conf.js by Kotlin/JS

const os = require('os');

// Set Chrome binary path explicitly for macOS
if (os.platform() === 'darwin') {
    process.env.CHROME_BIN = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
}

// Configure custom Chrome launcher with additional flags for stability
config.customLaunchers = {
    ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: [
            '--no-sandbox',
            '--disable-gpu',
            '--disable-dev-shm-usage',
            '--disable-software-rasterizer',
            '--disable-extensions'
        ]
    }
};

// Increase timeouts for NIST statistical tests (can take 60s+ per test)
config.browserNoActivityTimeout = 120000;  // 120 seconds (NIST tests are computationally intensive)
config.browserDisconnectTimeout = 10000;   // 10 seconds (default: 2000)
config.browserDisconnectTolerance = 3;      // Allow 3 disconnects (default: 0)
config.captureTimeout = 210000;             // 210 seconds (default: 60000)

// Configure Mocha for long-running NIST tests
config.client = config.client || {};
config.client.mocha = config.client.mocha || {};
config.client.mocha.timeout = 120000;       // 120 seconds for NIST tests (matches browserNoActivityTimeout)
