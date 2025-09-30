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
import java.util.*;

import de.felixhertweck.seatreservation.userManagment.dto.AdminUserCreationDto;

public class CSVToDto {

    private static final Set<String> DEFAULT_ROLES = Set.of("USER");
    private static final Set<String> DEFAULT_TAGS = Set.of("imported");

    private static final String CSV_SEPARATOR = ";";
    private static final String JSON_OUTPUT_FILE = "importer/output.json";
    private static final String CSV_INPUT_FILE = "importer/input.csv";

    public static void main(String[] args) {
        List<AdminUserCreationDto> users = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(CSV_INPUT_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(CSV_SEPARATOR);
                if (parts.length < 3) {
                    System.err.println("Skipping line: " + line);
                    continue;
                }
                String firstname = parts[0].trim();
                String lastname = parts[1].trim();
                String password = parts[2].trim();
                String email = null;
                if (parts.length >= 4) {
                    email = parts[3].trim();
                }
                String username = (firstname + "." + lastname).toLowerCase().replaceAll("\\s+", "");

                username =
                        username.replace("ä", "ae")
                                .replace("ö", "oe")
                                .replace("ü", "ue")
                                .replace("ß", "ss");

                if (firstname.isEmpty() || lastname.isEmpty() || password.isEmpty()) {
                    System.err.println("Skipping line due to empty fields: " + line);
                    continue;
                }

                AdminUserCreationDto dto =
                        new AdminUserCreationDto(
                                username,
                                email,
                                false,
                                password,
                                firstname,
                                lastname,
                                DEFAULT_ROLES,
                                DEFAULT_TAGS);
                users.add(dto);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
            return;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(JSON_OUTPUT_FILE))) {
            pw.println("[");
            for (int i = 0; i < users.size(); i++) {
                AdminUserCreationDto u = users.get(i);
                pw.print("  {");
                pw.print("\"username\": \"" + escape(u.getUsername()) + "\", ");
                pw.print("\"password\": \"" + escape(u.getPassword()) + "\", ");
                pw.print("\"firstname\": \"" + escape(u.getFirstname()) + "\", ");
                pw.print("\"lastname\": \"" + escape(u.getLastname()) + "\", ");

                // Rollen-Array
                pw.print("\"roles\": [");
                int rcount = 0;
                for (String r : u.getRoles()) {
                    if (rcount++ > 0) pw.print(", ");
                    pw.print("\"" + escape(r) + "\"");
                }
                pw.print("], ");

                // Tags-Array
                pw.print("\"tags\": [");
                int tcount = 0;
                for (String t : u.getTags()) {
                    if (tcount++ > 0) pw.print(", ");
                    pw.print("\"" + escape(t) + "\"");
                }
                pw.print("]");

                pw.print("}");

                if (i < users.size() - 1) pw.println(",");
                else pw.println();
            }
            pw.println("]");
        } catch (IOException e) {
            System.err.println("Error writing JSON: " + e.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
