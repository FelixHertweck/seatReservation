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
package de.felixhertweck.seatreservation.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import de.felixhertweck.seatreservation.userManagment.dto.AdminUserCreationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CSVToDtoTest {

    @TempDir Path tempDir;

    private Path tempCsvFile;

    @BeforeEach
    void setUp() {
        tempCsvFile = tempDir.resolve("test_users.csv");
    }

    @Test
    void testParseCsvWithHeader() throws IOException {
        String csvContent =
                "firstname;lastname;password;email\n"
                        + "John;Doe;secret123;john.doe@example.com\n"
                        + "Jane;Smith;pwd456;jane.smith@example.com\n";
        Files.writeString(tempCsvFile, csvContent, StandardCharsets.UTF_8);

        List<AdminUserCreationDto> users = CSVToDto.parseCsv(tempCsvFile);

        assertEquals(2, users.size());

        AdminUserCreationDto user1 = users.get(0);
        assertEquals("john.doe", user1.getUsername());
        assertEquals("john.doe@example.com", user1.getEmail());
        assertEquals("secret123", user1.getPassword());
        assertEquals("John", user1.getFirstname());
        assertEquals("Doe", user1.getLastname());

        AdminUserCreationDto user2 = users.get(1);
        assertEquals("jane.smith", user2.getUsername());
        assertEquals("jane.smith@example.com", user2.getEmail());
        assertEquals("pwd456", user2.getPassword());
        assertEquals("Jane", user2.getFirstname());
        assertEquals("Smith", user2.getLastname());
    }

    @Test
    void testParseCsvWithoutHeader() throws IOException {
        String csvContent =
                "Alice;Wonderland;alicePass;alice@example.com\n"
                        + "Bob;Builder;bobPass;bob@example.com\n";
        Files.writeString(tempCsvFile, csvContent, StandardCharsets.UTF_8);

        List<AdminUserCreationDto> users = CSVToDto.parseCsv(tempCsvFile);

        assertEquals(2, users.size());

        AdminUserCreationDto user1 = users.get(0);
        assertEquals("alice.wonderland", user1.getUsername());
        assertEquals("alice@example.com", user1.getEmail());

        AdminUserCreationDto user2 = users.get(1);
        assertEquals("bob.builder", user2.getUsername());
        assertEquals("bob@example.com", user2.getEmail());
    }

    @Test
    void testParseCsvMissingMandatoryFields() throws IOException {
        String csvContent =
                "firstname;lastname;password;email\n"
                        + ";Doe;secret123;john.doe@example.com\n" // missing firstname
                        + "John;;secret123;john.doe@example.com\n" // missing lastname
                        + "John;Doe;;john.doe@example.com\n" // missing password
                        + "John;Doe;secret123;\n"; // missing email (optional, should not skip)
        Files.writeString(tempCsvFile, csvContent, StandardCharsets.UTF_8);

        List<AdminUserCreationDto> users = CSVToDto.parseCsv(tempCsvFile);

        assertEquals(1, users.size());
        AdminUserCreationDto user = users.get(0);
        assertEquals("john.doe", user.getUsername());
        assertNull(user.getEmail());
    }

    @Test
    void testParseCsvSpecialCharactersInUsername() throws IOException {
        String csvContent =
                "firstname;lastname;password;email\n"
                        + "Müller;Gärtner;secret123;muller@example.com\n";
        Files.writeString(tempCsvFile, csvContent, StandardCharsets.UTF_8);

        List<AdminUserCreationDto> users = CSVToDto.parseCsv(tempCsvFile);

        assertEquals(1, users.size());
        AdminUserCreationDto user = users.get(0);
        // Müller -> mueller, Gärtner -> gaertner
        assertEquals("mueller.gaertner", user.getUsername());
    }
}
