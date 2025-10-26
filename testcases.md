# Test Cases Documentation

This document provides detailed documentation for test cases in the seat-reservation project.

## Table of Contents

- [HtmlSanitizerUtilsTest](#htmlsanitizerutilstest)
- [SecurityUtilsTest](#securityutilstest)

---

## HtmlSanitizerUtilsTest

**Package:** `de.felixhertweck.seatreservation.sanitization`  
**Class Under Test:** `HtmlSanitizerUtils`  
**Purpose:** Validates HTML sanitization functionality to prevent XSS (Cross-Site Scripting) attacks

### Test Cases (17 total)

#### Null and Empty Input Handling

##### `sanitize_ReturnsNullWhenInputIsNull`
- **Description:** Verifies that the sanitize method correctly handles null input
- **Input:** `null`
- **Expected Output:** `null`
- **Category:** Edge Case

##### `sanitize_ReturnsEmptyStringWhenInputIsEmpty`
- **Description:** Verifies that the sanitize method correctly handles empty string input
- **Input:** `""`
- **Expected Output:** `""`
- **Category:** Edge Case

##### `sanitize_PreservesPlainText`
- **Description:** Verifies that plain text without HTML is preserved as-is
- **Input:** `"This is plain text without any HTML"`
- **Expected Output:** Same as input
- **Category:** Positive Test

#### XSS Attack Prevention - Script Injection

##### `sanitize_RemovesScriptTags`
- **Description:** Verifies that script tags and their content are completely removed
- **Input:** `"<script>alert('XSS')</script>Hello"`
- **Expected Behavior:** Script tags and alert content removed
- **Category:** Security / XSS Prevention

##### `sanitize_RemovesOnEventHandlers`
- **Description:** Verifies that HTML event handlers (onclick, onerror, etc.) are removed
- **Input:** `"<div onclick='alert(1)'>Click me</div>"`
- **Expected Behavior:** Event handlers removed
- **Category:** Security / XSS Prevention

#### XSS Attack Prevention - Iframe and Embed Tags

##### `sanitize_RemovesIframeTags`
- **Description:** Verifies that iframe tags and their sources are removed
- **Input:** `"<iframe src='http://evil.com'></iframe>"`
- **Expected Behavior:** Iframe tags completely removed
- **Category:** Security / XSS Prevention

##### `sanitize_RemovesStyleTags`
- **Description:** Verifies that style tags are removed to prevent CSS-based attacks
- **Input:** `"<style>body { background: red; }</style>"`
- **Expected Behavior:** Style tags removed
- **Category:** Security / XSS Prevention

##### `sanitize_RemovesEmbedTags`
- **Description:** Verifies that embed tags are removed
- **Input:** `"<embed src='malicious.swf'>"`
- **Expected Behavior:** Embed tags removed
- **Category:** Security / XSS Prevention

##### `sanitize_RemovesObjectTags`
- **Description:** Verifies that object tags are removed
- **Input:** `"<object data='malicious.swf'></object>"`
- **Expected Behavior:** Object tags removed
- **Category:** Security / XSS Prevention

#### Protocol Filtering

##### `sanitize_RemovesAnchorTagsButAllowsMailtoProtocol`
- **Description:** Verifies that anchor tags are removed but text content is preserved
- **Input:** `"<a href='mailto:test@example.com'>Email</a>"`
- **Expected Output:** `"Email"`
- **Note:** The policy allows mailto protocol but doesn't allow anchor tags
- **Category:** Security / Protocol Filtering

##### `sanitize_RemovesHttpLinks`
- **Description:** Verifies that HTTP links in anchor tags are removed
- **Input:** `"<a href='http://example.com'>Link</a>"`
- **Expected Behavior:** HTTP links removed
- **Category:** Security / Protocol Filtering

##### `sanitize_RemovesHttpsLinks`
- **Description:** Verifies that HTTPS links in anchor tags are removed
- **Input:** `"<a href='https://example.com'>Link</a>"`
- **Expected Behavior:** HTTPS links removed
- **Category:** Security / Protocol Filtering

##### `sanitize_RemovesJavascriptProtocol`
- **Description:** Verifies that javascript: protocol URLs are removed
- **Input:** `"<a href='javascript:alert(1)'>Click</a>"`
- **Expected Behavior:** JavaScript protocol and content removed
- **Category:** Security / Protocol Filtering

##### `sanitize_RemovesDataProtocol`
- **Description:** Verifies that data: protocol URLs are removed
- **Input:** `"<a href='data:text/html,<script>alert(1)</script>'>Click</a>"`
- **Expected Behavior:** Data protocol removed
- **Category:** Security / Protocol Filtering

#### Complex Scenarios

##### `sanitize_HandlesMultipleXssAttempts`
- **Description:** Verifies that multiple XSS attack vectors in a single input are all removed
- **Input:** Multiple script, img, and iframe tags with various attack vectors
- **Expected Behavior:** All malicious content removed
- **Category:** Security / Integration Test

##### `sanitize_HandlesNestedTags`
- **Description:** Verifies that nested malicious tags are properly handled
- **Input:** `"<div><script>alert('XSS')</script><p>Text</p></div>"`
- **Expected Behavior:** Nested script tags removed
- **Category:** Security / Edge Case

##### `sanitize_HandlesMalformedHtml`
- **Description:** Verifies that malformed HTML is handled gracefully
- **Input:** `"<script>alert('XSS')<p>Unclosed"`
- **Expected Behavior:** Returns non-null result, script tags removed
- **Category:** Edge Case / Robustness

---

## SecurityUtilsTest

**Package:** `de.felixhertweck.seatreservation.utils`  
**Class Under Test:** `SecurityUtils`  
**Purpose:** Validates cryptographically secure random number generation functionality

### Test Cases (13 total)

#### SecureRandom Instance Management

##### `getSecureRandom_ReturnsNonNull`
- **Description:** Verifies that getSecureRandom returns a non-null SecureRandom instance
- **Expected Behavior:** Returns a valid SecureRandom object
- **Category:** Initialization

##### `getSecureRandom_ReturnsSameInstance`
- **Description:** Verifies the singleton pattern - multiple calls return the same instance
- **Expected Behavior:** Same instance returned on consecutive calls
- **Category:** Design Pattern / Singleton

#### Random Byte Generation

##### `generateRandomBytes_ReturnsCorrectLength`
- **Description:** Verifies that generated byte arrays have the requested length
- **Input:** Length of 16
- **Expected Output:** Byte array with exactly 16 elements
- **Category:** Functional Test

##### `generateRandomBytes_ReturnsNonNullArray`
- **Description:** Verifies that generateRandomBytes never returns null
- **Input:** Length of 10
- **Expected Output:** Non-null byte array
- **Category:** Null Safety

##### `generateRandomBytes_GeneratesRandomValues`
- **Description:** Verifies that consecutive calls generate different random values
- **Test Method:** Generates two 16-byte arrays and compares their content
- **Expected Behavior:** Arrays should have different content
- **Note:** Uses `Arrays.equals()` to compare byte array contents
- **Category:** Randomness Validation

##### `generateRandomBytes_HandlesDifferentLengths`
- **Description:** Verifies that the method handles various array sizes correctly
- **Test Cases:** 
  - Small (1 byte)
  - Medium (16 bytes)
  - Large (256 bytes)
- **Expected Behavior:** Correct length for each size
- **Category:** Boundary Testing

##### `generateRandomBytes_HandlesZeroLength`
- **Description:** Verifies that zero-length arrays are handled correctly
- **Input:** Length of 0
- **Expected Output:** Empty byte array (length 0)
- **Category:** Edge Case

#### Random Integer Generation

##### `nextInt_ReturnsValueWithinBound`
- **Description:** Verifies that generated integers are within the specified bound
- **Test Method:** Generates 100 random integers with bound 100
- **Expected Behavior:** All values >= 0 and < bound
- **Category:** Boundary Testing

##### `nextInt_GeneratesVariedValues`
- **Description:** Verifies that nextInt generates varied values, not always the same
- **Test Method:** Generates 100 random integers, counts unique values
- **Expected Behavior:** At least 50 unique values out of 100 generated
- **Category:** Randomness Validation

##### `nextInt_HandlesSmallBound`
- **Description:** Verifies correct behavior with small bounds (e.g., 2)
- **Input:** Bound of 2
- **Expected Output:** Values in range [0, 2)
- **Category:** Edge Case

#### Random Long Generation

##### `nextLong_GeneratesDifferentValues`
- **Description:** Verifies that consecutive nextLong calls return different values
- **Test Method:** Generates three long values
- **Expected Behavior:** Not all three values are identical
- **Category:** Randomness Validation

##### `nextLong_GeneratesVariedValues`
- **Description:** Verifies high variance in generated long values
- **Test Method:** Generates 100 long values, counts unique values
- **Expected Behavior:** At least 95 unique values out of 100
- **Category:** Randomness Validation

#### Utility Class Pattern

##### `constructor_ThrowsException`
- **Description:** Verifies that the utility class constructor cannot be instantiated
- **Test Method:** Uses reflection to access private constructor
- **Expected Behavior:** Constructor throws `UnsupportedOperationException` with message "Utility class cannot be instantiated"
- **Category:** Design Pattern / Utility Class

---

## Test Execution

To run these specific test suites:

```bash
# Run HtmlSanitizerUtilsTest
./mvnw test -Dtest=HtmlSanitizerUtilsTest

# Run SecurityUtilsTest
./mvnw test -Dtest=SecurityUtilsTest

# Run both
./mvnw test -Dtest=HtmlSanitizerUtilsTest,SecurityUtilsTest
```

## Coverage Notes

These tests are plain JUnit unit tests. The Quarkus jacoco configuration primarily tracks coverage through `@QuarkusTest` annotated integration tests. While these unit tests may not directly appear in the jacoco coverage report, they provide:

1. **Isolated validation** of utility functions without full application context
2. **Fast execution** without Quarkus overhead
3. **Security validation** ensuring critical security utilities work correctly
4. **Clear documentation** of expected behavior through test examples

The tested utilities (`HtmlSanitizerUtils` and `SecurityUtils`) are used by various services in the application that have their own integration tests, so their coverage appears indirectly in the overall jacoco report.

## Test Naming Convention

All tests follow the naming pattern: `methodName_scenario_expectedOutcome`

Examples:
- `sanitize_ReturnsNullWhenInputIsNull` - Tests the `sanitize` method when input is null, expects null return
- `generateRandomBytes_HandlesZeroLength` - Tests `generateRandomBytes` with zero length, expects correct handling
- `nextInt_ReturnsValueWithinBound` - Tests `nextInt` method, expects values within the specified bound
