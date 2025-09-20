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

class RandomUUIDStringTest {

    @Test
    void generate_ReturnsNonNullString() {
        String result = RandomUUIDString.generate();
        assertNotNull(result, "Generated UUID should not be null");
    }

    @Test
    void generate_ReturnsNonEmptyString() {
        String result = RandomUUIDString.generate();
        assertFalse(result.isEmpty(), "Generated UUID should not be empty");
    }

    @Test
    void generate_DoesNotContainUnderscores() {
        String result = RandomUUIDString.generate();
        assertFalse(result.contains("_"), "Generated UUID should not contain underscores");
    }

    @Test
    void generate_ReturnsValidLength() {
        String result = RandomUUIDString.generate();
        // Standard UUID is 36 characters with hyphens, no underscores to remove
        // So length should be 36
        assertEquals(36, result.length(), "Generated UUID should have expected length");
    }

    @Test
    void generate_ContainsHyphens() {
        String result = RandomUUIDString.generate();
        assertTrue(result.contains("-"), "Generated UUID should contain hyphens");
    }

    @Test
    void generate_ReturnsUniqueValues() {
        Set<String> generatedUuids = new HashSet<>();
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            String uuid = RandomUUIDString.generate();
            generatedUuids.add(uuid);
        }

        assertEquals(iterations, generatedUuids.size(), "All generated UUIDs should be unique");
    }

    @Test
    void generate_FollowsUUIDFormat() {
        String result = RandomUUIDString.generate();

        // UUID format: 8-4-4-4-12 characters separated by hyphens
        String[] parts = result.split("-");
        assertEquals(5, parts.length, "UUID should have 5 parts separated by hyphens");
        assertEquals(8, parts[0].length(), "First part should be 8 characters");
        assertEquals(4, parts[1].length(), "Second part should be 4 characters");
        assertEquals(4, parts[2].length(), "Third part should be 4 characters");
        assertEquals(4, parts[3].length(), "Fourth part should be 4 characters");
        assertEquals(12, parts[4].length(), "Fifth part should be 12 characters");
    }

    @Test
    void generate_ContainsOnlyValidHexCharacters() {
        String result = RandomUUIDString.generate();
        String withoutHyphens = result.replace("-", "");

        assertTrue(
                withoutHyphens.matches("[0-9a-f]+"),
                "UUID should contain only valid hexadecimal characters (lowercase)");
    }
}
