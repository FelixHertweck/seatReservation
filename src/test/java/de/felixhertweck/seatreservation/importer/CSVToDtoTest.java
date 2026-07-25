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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CSVToDtoTest {

    @TempDir Path tempDir;

    private Path csvFile;
    private Path jsonFile;

    @BeforeEach
    void setUp() {
        csvFile = tempDir.resolve("input.csv");
        jsonFile = tempDir.resolve("output.json");
    }

    @AfterEach
    void tearDown() {
        // Clear system properties if set by tests to avoid pollution
        System.clearProperty("importer.input");
        System.clearProperty("importer.output");
    }

    @Test
    void run_inputDoesNotExist_returnsExitCode2() {
        String[] args =
                new String[] {tempDir.resolve("non-existent.csv").toString(), jsonFile.toString()};
        int exitCode = CSVToDto.run(args);
        assertEquals(2, exitCode);
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_happyPathWithHeader_returnsExitCode0AndWritesJson() throws IOException {
        String csvContent =
                "firstname;lastname;password;email\n"
                        + "Max;Mustermann;secret123;max@example.com\n"
                        + "Erika;Musterfrau;secret456;erika@example.com\n";
        Files.writeString(csvFile, csvContent);

        String[] args = new String[] {csvFile.toString(), jsonFile.toString()};
        int exitCode = CSVToDto.run(args);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(jsonFile));

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> list =
                mapper.readValue(
                        jsonFile.toFile(), new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(2, list.size());

        Map<String, Object> max = list.get(0);
        assertEquals("max.mustermann", max.get("username"));
        assertEquals("max@example.com", max.get("email"));
        assertEquals("secret123", max.get("password"));
        assertEquals("Max", max.get("firstname"));
        assertEquals("Mustermann", max.get("lastname"));
        assertEquals(Boolean.FALSE, max.get("sendEmailVerification"));
        assertEquals(List.of("USER"), max.get("roles"));
        assertEquals(List.of("imported"), max.get("tags"));

        Map<String, Object> erika = list.get(1);
        assertEquals("erika.musterfrau", erika.get("username"));
        assertEquals("erika@example.com", erika.get("email"));
        assertEquals("secret456", erika.get("password"));
        assertEquals("Erika", erika.get("firstname"));
        assertEquals("Musterfrau", erika.get("lastname"));
    }

    @Test
    void run_happyPathWithoutHeader_returnsExitCode0AndProcessesFirstRecord() throws IOException {
        String csvContent =
                "Max;Mustermann;secret123;max@example.com\n"
                        + "Erika;Musterfrau;secret456;erika@example.com\n";
        Files.writeString(csvFile, csvContent);

        String[] args = new String[] {csvFile.toString(), jsonFile.toString()};
        int exitCode = CSVToDto.run(args);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(jsonFile));

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> list =
                mapper.readValue(
                        jsonFile.toFile(), new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(2, list.size());
    }

    @Test
    void run_skipsMissingMandatoryFields_returnsExitCode0() throws IOException {
        String csvContent =
                "firstname;lastname;password;email\n"
                        + ";Mustermann;secret123;max@example.com\n" // missing firstname
                        + "Max;;secret123;max@example.com\n" // missing lastname
                        + "Max;Mustermann;;max@example.com\n" // missing password
                        + "Erika;Musterfrau;secret456;\n"; // valid but missing optional email
        Files.writeString(csvFile, csvContent);

        String[] args = new String[] {csvFile.toString(), jsonFile.toString()};
        int exitCode = CSVToDto.run(args);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(jsonFile));

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> list =
                mapper.readValue(
                        jsonFile.toFile(), new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(1, list.size());
        Map<String, Object> erika = list.get(0);
        assertEquals("erika.musterfrau", erika.get("username"));
        assertNull(erika.get("email"));
    }

    @Test
    void run_specialCharactersAndLongUsername_returnsExitCode0AndHandlesUsernameSizing()
            throws IOException {
        // Username normalization should replace umlauts, remove invalid characters, trim white
        // spaces and truncate to 64 chars
        String csvContent =
                "firstname;lastname;password;email\n"
                    + "Jäcoß "
                    + " ;Müller-Schmidt-With-Very-Very-Very-Very-Very-Long-Last-Name;secret123;j@example.com\n";
        Files.writeString(csvFile, csvContent);

        String[] args = new String[] {csvFile.toString(), jsonFile.toString()};
        int exitCode = CSVToDto.run(args);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(jsonFile));

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> list =
                mapper.readValue(
                        jsonFile.toFile(), new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(1, list.size());
        Map<String, Object> user = list.get(0);
        String username = (String) user.get("username");
        assertTrue(username.length() <= 64);
        // "jäcoß" -> "jaecoss", "müller-schmidt-with-very..." -> "mueller-schmidt-with-very..."
        // Expected username starts with jaecoss.mueller-schmidt-
        assertTrue(username.startsWith("jaecoss.mueller-schmidt"));
    }

    @Test
    void run_ioExceptionWritingJson_returnsExitCode4() throws IOException {
        String csvContent =
                "firstname;lastname;password;email\n"
                        + "Max;Mustermann;secret123;max@example.com\n";
        Files.writeString(csvFile, csvContent);

        // Making the output file a directory, causing Jackson to fail with IOException when trying
        // to write to it
        Path invalidJsonPath = tempDir.resolve("invalid_json_dir");
        Files.createDirectories(invalidJsonPath);

        String[] args = new String[] {csvFile.toString(), invalidJsonPath.toString()};
        int exitCode = CSVToDto.run(args);

        assertEquals(4, exitCode);
    }

    @Test
    void run_fallbacksToSystemProperties() throws IOException {
        String csvContent =
                "firstname;lastname;password;email\n"
                        + "Max;Mustermann;secret123;max@example.com\n";
        Files.writeString(csvFile, csvContent);

        System.setProperty("importer.input", csvFile.toString());
        System.setProperty("importer.output", jsonFile.toString());

        // Call run with empty arguments, relying on system properties
        int exitCode = CSVToDto.run(new String[0]);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(jsonFile));
    }
}
