/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.felixhertweck.seatreservation.utils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VerificationCodeGeneratorTest {

    @Test
    void generate_ReturnsNonNullString() {
        String result = VerificationCodeGenerator.generate();
        assertNotNull(result, "Generated code should not be null");
    }

    @Test
    void generate_ReturnsNonEmptyString() {
        String result = VerificationCodeGenerator.generate();
        assertFalse(result.isEmpty(), "Generated code should not be empty");
    }

    @Test
    void generate_Returns6DigitCode() {
        String result = VerificationCodeGenerator.generate();
        assertEquals(6, result.length(), "Generated code should be exactly 6 digits");
    }

    @Test
    void generate_ContainsOnlyDigits() {
        String result = VerificationCodeGenerator.generate();
        assertTrue(result.matches("\\d{6}"), "Generated code should contain only 6 digits");
    }

    @Test
    void generate_ReturnsCodesInValidRange() {
        for (int i = 0; i < 100; i++) {
            String result = VerificationCodeGenerator.generate();
            int code = Integer.parseInt(result);
            assertTrue(code >= 100000, "Generated code should be >= 100000");
            assertTrue(code <= 999999, "Generated code should be <= 999999");
        }
    }

    @Test
    void generate_ReturnsUniqueValues() {
        Set<String> generatedCodes = new HashSet<>();
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            String code = VerificationCodeGenerator.generate();
            generatedCodes.add(code);
        }

        // Even with a limited range (100000-999999 = 900000 possible codes),
        // we should get mostly unique values in 1000 iterations
        assertTrue(
                generatedCodes.size() > 900,
                "Most generated codes should be unique (got "
                        + generatedCodes.size()
                        + " unique out of "
                        + iterations
                        + ")");
    }

    @Test
    void generate_DoesNotStartWithZero() {
        for (int i = 0; i < 100; i++) {
            String result = VerificationCodeGenerator.generate();
            assertFalse(result.startsWith("0"), "6-digit code should not start with 0");
        }
    }

    @Test
    void generate_GeneratesExpectedFormat() {
        String result = VerificationCodeGenerator.generate();

        // Should be exactly 6 digits
        assertTrue(
                result.matches("^[1-9]\\d{5}$"),
                "Code should be 6 digits starting with 1-9, got: " + result);
    }
}
