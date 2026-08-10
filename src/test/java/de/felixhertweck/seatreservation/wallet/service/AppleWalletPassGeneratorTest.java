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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.wallet.dto.WalletPassData;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AppleWalletPassGeneratorTest {

    @Inject AppleWalletPassGenerator appleWalletPassGenerator;

    @Test
    void testGeneratePassCreatesUnsignedPkpassWhenCertificatesMissing() throws Exception {
        WalletPassData data =
                new WalletPassData(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Summer Concert",
                        "Live concert on main stage",
                        Instant.now(),
                        Instant.now().plusSeconds(7200),
                        "Main Stage",
                        "123 Concert Hall Ave",
                        "Row A, Seat 12",
                        "VIP Area",
                        "Row A",
                        "Seat 12",
                        UUID.randomUUID(),
                        "Alice Smith",
                        "alice@example.com",
                        "valid-token",
                        "qr-payload-string");

        WalletPassResponseDTO response = appleWalletPassGenerator.generatePass(data);

        assertNotNull(response);
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
                "Unsigned archive must not contain signature file");

        String passJsonStr = new String(entries.get("pass.json"));
        assertTrue(passJsonStr.contains("Summer Concert"));
        assertTrue(passJsonStr.contains("Main Stage"));
        assertTrue(passJsonStr.contains("Alice Smith"));
    }
}
