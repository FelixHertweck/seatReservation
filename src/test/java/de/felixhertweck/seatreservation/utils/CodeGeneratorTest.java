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

class CodeGeneratorTest {

    @Test
    void generateRandomCode_ReturnsNonNullString() {
        String code = CodeGenerator.generateRandomCode();
        assertNotNull(code, "Generated code should not be null");
    }

    @Test
    void generateRandomCode_ReturnsNonEmptyString() {
        String code = CodeGenerator.generateRandomCode();
        assertFalse(code.isEmpty(), "Generated code should not be empty");
    }

    @Test
    void generateRandomCode_ReturnsCorrectLength() {
        String code = CodeGenerator.generateRandomCode();
        assertEquals(4, code.length(), "Generated code should have length 4");
    }

    @Test
    void generateRandomCode_ContainsOnlyAllowedCharacters() {
        String code = CodeGenerator.generateRandomCode();
        assertTrue(
                code.matches("^[A-Z0-9]{4}$"),
                "Generated code should only contain uppercase letters and digits, got: " + code);
    }

    @Test
    void generateRandomCode_GeneratesSufficientlyUniqueCodes() {
        Set<String> codes = new HashSet<>();
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            codes.add(CodeGenerator.generateRandomCode());
        }

        // With 36^4 = 1,679,616 possible combinations,
        // the probability of collisions in 1000 samples is very low.
        // We expect almost all to be unique.
        assertTrue(
                codes.size() > 990,
                "Expected high uniqueness, but got only "
                        + codes.size()
                        + " unique codes out of "
                        + iterations);
    }
}
