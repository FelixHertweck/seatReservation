/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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

    @Test
    @SuppressWarnings("unchecked")
    void testHappyPathWithHeader(@TempDir Path tempDir) throws IOException {
        Path input = tempDir.resolve("input.csv");
        Path output = tempDir.resolve("output.json");

        String csvContent =
                "firstname;lastname;password;email\n"
                        + "John;Doe;secret123;john.doe@example.com\n"
                        + " Jane ; Smith ; pwd456 ; jane.smith@example.com \n";
        Files.writeString(input, csvContent);

        int exitCode = CSVToDto.runImporter(new String[] {input.toString(), output.toString()});

        assertEquals(0, exitCode, "Should return exit code 0 on success");
        assertTrue(Files.exists(output), "Output JSON should be created");

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> users =
                mapper.readValue(
                        output.toFile(), new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(2, users.size());
        assertEquals("john.doe", users.get(0).get("username"));
        assertEquals("john.doe@example.com", users.get(0).get("email"));
        assertEquals("secret123", users.get(0).get("password"));
        assertEquals("John", users.get(0).get("firstname"));
        assertEquals("Doe", users.get(0).get("lastname"));
        assertFalse((Boolean) users.get(0).get("sendEmailVerification"));
        assertTrue(((List<String>) users.get(0).get("roles")).contains("USER"));
        assertTrue(((List<String>) users.get(0).get("tags")).contains("imported"));

        assertEquals("jane.smith", users.get(1).get("username"));
        assertEquals("jane.smith@example.com", users.get(1).get("email"));
        assertEquals("pwd456", users.get(1).get("password"));
        assertEquals("Jane", users.get(1).get("firstname"));
        assertEquals("Smith", users.get(1).get("lastname"));
    }

    @Test
    void testHappyPathWithoutHeader(@TempDir Path tempDir) throws IOException {
        Path input = tempDir.resolve("input.csv");
        Path output = tempDir.resolve("output.json");

        String csvContent =
                "John;Doe;secret123;john.doe@example.com\n"
                        + "Jane;Smith;pwd456;jane.smith@example.com\n";
        Files.writeString(input, csvContent);

        int exitCode = CSVToDto.runImporter(new String[] {input.toString(), output.toString()});

        assertEquals(0, exitCode);
        assertTrue(Files.exists(output));

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> users =
                mapper.readValue(
                        output.toFile(), new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(2, users.size());
        assertEquals("john.doe", users.get(0).get("username"));
        assertEquals("jane.smith", users.get(1).get("username"));
    }

    @Test
    void testSkipInvalidRecord(@TempDir Path tempDir) throws IOException {
        Path input = tempDir.resolve("input.csv");
        Path output = tempDir.resolve("output.json");

        String csvContent =
                "firstname;lastname;password;email\n"
                        + ";Doe;secret123;john.doe@example.com\n"
                        + "Jane;;pwd456;jane.smith@example.com\n"
                        + "Bob;Builder;;bob@example.com\n"
                        + "Valid;User;pwd789;valid@example.com\n";
        Files.writeString(input, csvContent);

        int exitCode = CSVToDto.runImporter(new String[] {input.toString(), output.toString()});

        assertEquals(0, exitCode);
        assertTrue(Files.exists(output));

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> users =
                mapper.readValue(
                        output.toFile(), new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(1, users.size());
        assertEquals("valid.user", users.get(0).get("username"));
    }

    @Test
    void testNonExistentInputFile(@TempDir Path tempDir) {
        Path input = tempDir.resolve("non-existent.csv");
        Path output = tempDir.resolve("output.json");

        int exitCode = CSVToDto.runImporter(new String[] {input.toString(), output.toString()});

        assertEquals(2, exitCode, "Should return exit code 2 when input file does not exist");
    }

    @Test
    void testReadCsvErrorPath(@TempDir Path tempDir) throws IOException {
        // In Java, trying to read a directory as a file via BufferedReader will throw an
        // IOException or UncheckedIOException
        Path inputDir = tempDir.resolve("input_dir");
        Files.createDirectories(inputDir);
        Path output = tempDir.resolve("output.json");

        int exitCode = CSVToDto.runImporter(new String[] {inputDir.toString(), output.toString()});

        assertEquals(
                3, exitCode, "Should return exit code 3 when IOException occurs while reading CSV");
    }

    @Test
    void testWriteJsonErrorPath(@TempDir Path tempDir) throws IOException {
        Path input = tempDir.resolve("input.csv");
        String csvContent =
                "firstname;lastname;password;email\n" + "John;Doe;secret123;john.doe@example.com\n";
        Files.writeString(input, csvContent);

        // Making the output file path be a directory will cause Jackson writeValue to throw an
        // IOException
        Path outputDir = tempDir.resolve("output_dir");
        Files.createDirectories(outputDir);

        int exitCode = CSVToDto.runImporter(new String[] {input.toString(), outputDir.toString()});

        assertEquals(
                4,
                exitCode,
                "Should return exit code 4 when IOException occurs while writing JSON");
    }
}
