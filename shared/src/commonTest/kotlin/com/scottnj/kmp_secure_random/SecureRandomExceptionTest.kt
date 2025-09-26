package com.scottnj.kmp_secure_random

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the exception hierarchy used in secure random operations.
 */
class SecureRandomExceptionTest {

    @Test
    fun testSecureRandomExceptionBasic() {
        val exception = SecureRandomException("Test message")

        assertEquals("Test message", exception.message)
        assertNull(exception.cause)
        assertTrue(exception is Exception)
    }

    @Test
    fun testSecureRandomExceptionWithCause() {
        val cause = RuntimeException("Original cause")
        val exception = SecureRandomException("Test message", cause)

        assertEquals("Test message", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testSecureRandomInitializationException() {
        val exception = SecureRandomInitializationException("Initialization failed")

        assertEquals("Initialization failed", exception.message)
        assertTrue(exception is SecureRandomException)
        assertNull(exception.cause)
    }

    @Test
    fun testSecureRandomInitializationExceptionWithCause() {
        val cause = IllegalStateException("System API unavailable")
        val exception = SecureRandomInitializationException("Initialization failed", cause)

        assertEquals("Initialization failed", exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is SecureRandomException)
    }

    @Test
    fun testSecureRandomGenerationException() {
        val exception = SecureRandomGenerationException("Generation failed")

        assertEquals("Generation failed", exception.message)
        assertTrue(exception is SecureRandomException)
        assertNull(exception.cause)
    }

    @Test
    fun testSecureRandomGenerationExceptionWithCause() {
        val cause = IllegalStateException("Hardware failure")
        val exception = SecureRandomGenerationException("Generation failed", cause)

        assertEquals("Generation failed", exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is SecureRandomException)
    }

    @Test
    fun testInvalidParameterException() {
        val exception = InvalidParameterException("must be positive", "bound", -5)

        assertTrue(exception.message?.contains("Invalid parameter 'bound' with value '-5': must be positive") == true)
        assertEquals("bound", exception.parameterName)
        assertEquals(-5, exception.parameterValue)
        assertTrue(exception is SecureRandomException)
    }

    @Test
    fun testInvalidParameterExceptionWithNullValue() {
        val exception = InvalidParameterException("cannot be null", "array", null)

        assertTrue(exception.message?.contains("Invalid parameter 'array' with value 'null': cannot be null") == true)
        assertEquals("array", exception.parameterName)
        assertNull(exception.parameterValue)
    }

    @Test
    fun testUnsupportedPlatformException() {
        val exception = UnsupportedPlatformException("Feature not implemented", "TestPlatform")

        assertTrue(exception.message?.contains("Unsupported operation on platform 'TestPlatform': Feature not implemented") == true)
        assertEquals("TestPlatform", exception.platformName)
        assertTrue(exception is SecureRandomException)
    }

    @Test
    fun testInsufficientResourcesException() {
        val exception = InsufficientResourcesException("Not enough entropy available", "entropy")

        assertTrue(exception.message?.contains("Insufficient entropy: Not enough entropy available") == true)
        assertEquals("entropy", exception.resourceType)
        assertTrue(exception is SecureRandomException)
    }

    @Test
    fun testExceptionHierarchy() {
        val exceptions = listOf(
            SecureRandomInitializationException("test"),
            SecureRandomGenerationException("test"),
            InvalidParameterException("test", "param", 0),
            UnsupportedPlatformException("test", "platform"),
            InsufficientResourcesException("test", "resource")
        )

        exceptions.forEach { exception ->
            assertTrue(exception is SecureRandomException, "${exception::class.simpleName} should extend SecureRandomException")
            assertTrue(exception is Exception, "${exception::class.simpleName} should extend Exception")
            assertNotNull(exception.message, "${exception::class.simpleName} should have a message")
        }
    }

    @Test
    fun testExceptionMessagesAreDescriptive() {
        val initException = SecureRandomInitializationException("Platform API not available")
        val genException = SecureRandomGenerationException("Entropy source exhausted")
        val paramException = InvalidParameterException("must be positive", "bound", -1)
        val platformException = UnsupportedPlatformException("Feature not available", "JS")
        val resourceException = InsufficientResourcesException("System low on entropy", "entropy")

        assertTrue(initException.message?.contains("Platform API not available") == true)
        assertTrue(genException.message?.contains("Entropy source exhausted") == true)
        assertTrue(paramException.message?.contains("must be positive") == true)
        assertTrue(platformException.message?.contains("Feature not available") == true)
        assertTrue(resourceException.message?.contains("System low on entropy") == true)
    }
}