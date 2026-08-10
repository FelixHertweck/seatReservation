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
package de.felixhertweck.seatreservation.wallet.service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.wallet.dto.WalletPassData;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GenericPkpassGeneratorTest {

    @Inject GenericPkpassGenerator genericPkpassGenerator;

    @Test
    void testGetProvider() {
        assertEquals(WalletProvider.GENERIC_PKPASS, genericPkpassGenerator.getProvider());
    }

    @Test
    void testGeneratePassCreatesUnsignedGenericPkpass() throws Exception {
        WalletPassData data =
                new WalletPassData(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Tech Conference",
                        "Annual Developer Gathering",
                        Instant.now(),
                        Instant.now().plusSeconds(7200),
                        "Hall B",
                        "456 Tech Park Way",
                        "Row C, Seat 5",
                        "General Section",
                        "Row C",
                        "Seat 5",
                        UUID.randomUUID(),
                        "Bob Johnson",
                        "bob@example.com",
                        "token-xyz",
                        "qr-content-bob");

        WalletPassResponseDTO response = genericPkpassGenerator.generatePass(data);

        assertNotNull(response);
        assertEquals(WalletProvider.GENERIC_PKPASS, response.provider());
        assertNotNull(response.content());
        assertTrue(response.filename().endsWith(".pkpass"));

        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis =
                new ZipInputStream(new ByteArrayInputStream(response.content()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), zis.readAllBytes());
                zis.closeEntry();
            }
        }

        assertTrue(entries.containsKey("pass.json"), "Archive must contain pass.json");
        assertTrue(entries.containsKey("manifest.json"), "Archive must contain manifest.json");
        assertFalse(
                entries.containsKey("signature"),
                "Generic archive must not contain signature file");

        String passJsonStr = new String(entries.get("pass.json"));
        assertTrue(passJsonStr.contains("Tech Conference"));
        assertTrue(passJsonStr.contains("Hall B"));
        assertTrue(passJsonStr.contains("Bob Johnson"));
    }

    @Test
    void testGeneratePassWithMultipleSeatsBundlesAllSeats() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WalletPassData seat1 =
                new WalletPassData(
                        UUID.randomUUID(),
                        eventId,
                        "Gala Night",
                        "Annual Gala",
                        now,
                        now.plusSeconds(3600),
                        "Main Hall",
                        "Main St 1",
                        "Row A, Seat 1",
                        "VIP",
                        "Row A",
                        "Seat 1",
                        userId,
                        "Alice Smith",
                        "alice@example.com",
                        "token-123",
                        "qr-content-123");

        WalletPassData seat2 =
                new WalletPassData(
                        UUID.randomUUID(),
                        eventId,
                        "Gala Night",
                        "Annual Gala",
                        now,
                        now.plusSeconds(3600),
                        "Main Hall",
                        "Main St 1",
                        "Row A, Seat 2",
                        "VIP",
                        "Row A",
                        "Seat 2",
                        userId,
                        "Alice Smith",
                        "alice@example.com",
                        "token-123",
                        "qr-content-123");

        WalletPassResponseDTO response =
                genericPkpassGenerator.generatePass(java.util.List.of(seat1, seat2));

        assertNotNull(response);
        assertEquals("application/vnd.apple.pkpasses", response.contentType());
        assertTrue(response.filename().endsWith(".pkpasses"));

        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis =
                new ZipInputStream(new ByteArrayInputStream(response.content()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), zis.readAllBytes());
                zis.closeEntry();
            }
        }

        assertEquals(2, entries.size());
        assertTrue(entries.keySet().stream().anyMatch(k -> k.endsWith(".pkpass")));
    }
}
