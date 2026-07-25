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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CSVToDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void run_WithValidCSVAndHeader_ReturnsZeroAndWritesJSON(@TempDir Path tempDir)
            throws IOException {
        Path inputCSV = tempDir.resolve("input.csv");
        Path outputJSON = tempDir.resolve("output.json");

        String csvContent =
                "firstname;lastname;password;email\n"
                        + "John;Doe;secret123;john.doe@example.com\n"
                        + "Jane;Smith;pwd456;jane.smith@example.com\n"
                        + "Max;Müller;umlautPass;max.mueller@example.com\n"
                        + "  ;MissingFirstname;nopass;email@example.com\n"; // should skip due to
        // missing mandatory
        // fields

        Files.writeString(inputCSV, csvContent);

        int exitCode = CSVToDto.run(new String[] {inputCSV.toString(), outputJSON.toString()});

        assertEquals(0, exitCode, "CSVToDto.run should return 0 on successful processing");
        assertTrue(Files.exists(outputJSON), "Output JSON file should be created");

        // Deserialize as Map to avoid direct DTO instantiation issue
        List<Map<String, Object>> users =
                mapper.readValue(
                        outputJSON.toFile(), new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(3, users.size(), "Only three valid users should be processed");

        // Assert first user
        Map<String, Object> user1 = users.get(0);
        assertEquals("john.doe", user1.get("username"));
        assertEquals("John", user1.get("firstname"));
        assertEquals("Doe", user1.get("lastname"));
        assertEquals("secret123", user1.get("password"));
        assertEquals("john.doe@example.com", user1.get("email"));
        assertEquals(Boolean.FALSE, user1.get("sendEmailVerification"));
        assertEquals(List.of("USER"), user1.get("roles"));
        assertEquals(List.of("imported"), user1.get("tags"));

        // Assert third user (with German umlaut replaced in username)
        Map<String, Object> user3 = users.get(2);
        assertEquals("max.mueller", user3.get("username"));
        assertEquals("Max", user3.get("firstname"));
        assertEquals("Müller", user3.get("lastname"));
    }

    @Test
    void run_WithValidCSVNoHeader_ReturnsZeroAndWritesJSON(@TempDir Path tempDir)
            throws IOException {
        Path inputCSV = tempDir.resolve("input_no_header.csv");
        Path outputJSON = tempDir.resolve("output.json");

        String csvContent = "Alice;Wonderland;alice123;alice@example.com\n";
        Files.writeString(inputCSV, csvContent);

        int exitCode = CSVToDto.run(new String[] {inputCSV.toString(), outputJSON.toString()});

        assertEquals(0, exitCode, "CSVToDto.run should return 0 even without header");
        assertTrue(Files.exists(outputJSON), "Output JSON file should be created");

        List<Map<String, Object>> users =
                mapper.readValue(
                        outputJSON.toFile(), new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(1, users.size());

        Map<String, Object> user = users.get(0);
        assertEquals("alice.wonderland", user.get("username"));
        assertEquals("Alice", user.get("firstname"));
        assertEquals("Wonderland", user.get("lastname"));
    }

    @Test
    void run_WithNonExistentInputFile_ReturnsTwo(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("doesnotexist.csv");
        Path outputJSON = tempDir.resolve("output.json");

        int exitCode = CSVToDto.run(new String[] {nonExistent.toString(), outputJSON.toString()});

        assertEquals(2, exitCode, "CSVToDto.run should return 2 if input file does not exist");
    }

    @Test
    void run_WithCSVReadingError_ReturnsThree(@TempDir Path tempDir) {
        // Passing a directory path as the CSV file should fail to read
        Path directoryAsInput = tempDir;
        Path outputJSON = tempDir.resolve("output.json");

        int exitCode =
                CSVToDto.run(new String[] {directoryAsInput.toString(), outputJSON.toString()});

        assertEquals(3, exitCode, "CSVToDto.run should return 3 on reading/parsing failure");
    }

    @Test
    void run_WithJSONWritingError_ReturnsFour(@TempDir Path tempDir) throws IOException {
        Path inputCSV = tempDir.resolve("input.csv");
        Files.writeString(inputCSV, "John;Doe;secret123;john.doe@example.com\n");

        // Passing a directory path as the output JSON file should trigger IOException during JSON
        // writing
        Path directoryAsOutput = tempDir;

        int exitCode =
                CSVToDto.run(new String[] {inputCSV.toString(), directoryAsOutput.toString()});

        assertEquals(4, exitCode, "CSVToDto.run should return 4 on JSON writing failure");
    }
}
