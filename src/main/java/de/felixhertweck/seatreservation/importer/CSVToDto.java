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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserCreationDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Simple CSV -> JSON importer for AdminUserCreationDto.
 *
 * <p>Usage: java -jar ... CSVToDto [input.csv] [output.json] If no arguments are provided, defaults
 * to 'importer/input.csv' and 'importer/output.json'.
 */
public class CSVToDto {

    private static final Set<String> DEFAULT_ROLES = Set.of("USER");
    private static final Set<String> DEFAULT_TAGS = Set.of("imported");
    private static final String DEFAULT_CSV = "importer/input.csv";
    private static final String DEFAULT_JSON = "importer/output.json";

    public static void main(String[] args) {
        String input =
                args.length > 0 ? args[0] : System.getProperty("importer.input", DEFAULT_CSV);
        String output =
                args.length > 1 ? args[1] : System.getProperty("importer.output", DEFAULT_JSON);

        List<AdminUserCreationDto> users = new ArrayList<>();

        Path inputPath = Path.of(input);
        if (!Files.exists(inputPath)) {
            System.err.println("Input file does not exist: " + inputPath.toAbsolutePath());
            System.exit(2);
        }

        CSVFormat format =
                CSVFormat.DEFAULT
                        .builder()
                        .setDelimiter(';')
                        .setIgnoreSurroundingSpaces(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build();

        try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
                CSVParser csv = new CSVParser(reader, format)) {

            Iterator<CSVRecord> it = csv.iterator();
            if (it.hasNext()) {
                CSVRecord first = it.next();
                if (looksLikeHeader(first)) {
                    // header detected -> ignore
                } else {
                    // process first record as data
                    processRecord(first, users);
                }
            }

            while (it.hasNext()) {
                CSVRecord record = it.next();
                // Expecting at least firstname, lastname, password; optional email
                processRecord(record, users);
            }

        } catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }

        // Write JSON using Jackson
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Path outPath = Path.of(output);
        try {
            Files.createDirectories(
                    outPath.getParent() == null ? Path.of(".") : outPath.getParent());
            mapper.writeValue(outPath.toFile(), users);
            System.out.println("Wrote " + users.size() + " users to " + outPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing JSON: " + e.getMessage());
            e.printStackTrace();
            System.exit(4);
        }
    }

    private static String safeGet(CSVRecord record, String header, int index) {
        try {
            if (record.isMapped(header)) {
                String v = record.get(header);
                return v == null ? "" : v;
            }
        } catch (IllegalArgumentException ignored) {
        }
        // Fallback by index
        if (index < record.size()) {
            String v = record.get(index);
            return v == null ? "" : v;
        }
        return "";
    }

    private static void processRecord(CSVRecord record, List<AdminUserCreationDto> users) {
        String firstname = safeGet(record, "firstname", 0);
        String lastname = safeGet(record, "lastname", 1);
        String password = safeGet(record, "password", 2);
        String email = safeGet(record, "email", 3);

        if (firstname.isBlank() || lastname.isBlank() || password.isBlank()) {
            System.err.println("Skipping record due missing mandatory fields: " + record);
            return;
        }

        String username = createUsername(firstname, lastname);

        AdminUserCreationDto dto =
                new AdminUserCreationDto(
                        username,
                        email.isBlank() ? null : email,
                        Boolean.FALSE,
                        password,
                        firstname,
                        lastname,
                        DEFAULT_ROLES,
                        DEFAULT_TAGS);
        users.add(dto);
    }

    private static boolean looksLikeHeader(CSVRecord record) {
        // simple heuristic: if any column name contains non-letter characters or matches known
        // headers
        for (String v : toList(record)) {
            String lower = v.toLowerCase(Locale.ROOT).trim();
            if (lower.isEmpty()) return false;
            if (lower.matches(".*(firstname|lastname|password|email).*")) return true;
        }
        return false;
    }

    private static List<String> toList(CSVRecord record) {
        List<String> l = new ArrayList<>(record.size());
        for (int i = 0; i < record.size(); i++) l.add(record.get(i));
        return l;
    }

    private static String createUsername(String firstname, String lastname) {
        String username = (firstname + "." + lastname).toLowerCase().replaceAll("\\s+", "");
        username =
                username.replace("ä", "ae")
                        .replace("ö", "oe")
                        .replace("ü", "ue")
                        .replace("ß", "ss");
        // remove characters not allowed by AdminUserCreationDto pattern
        username = username.replaceAll("[^a-z0-9._-]", "");
        if (username.length() > 64) username = username.substring(0, 64);
        return username;
    }
}
