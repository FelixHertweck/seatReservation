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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassData;

/**
 * Base class for PKPASS generators. Encapsulates common pass.json construction, manifest SHA-1
 * generation, and ZIP archive creation.
 */
public abstract class AbstractPkpassGenerator extends AbstractWalletPassGenerator {

    protected static final String KEY_LABEL = "label";
    protected static final String KEY_VALUE = "value";

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected byte[] buildPassJson(WalletPassData data, String passTypeIdentifier, String teamId)
            throws IOException {
        return buildPassJson(List.of(data), passTypeIdentifier, teamId);
    }

    protected byte[] buildPassJson(
            List<WalletPassData> passes, String passTypeIdentifier, String teamId)
            throws IOException {
        if (passes == null || passes.isEmpty()) {
            throw new IllegalArgumentException("Passes list must not be empty");
        }
        WalletPassData first = passes.get(0);

        Map<String, Object> passJson = new HashMap<>();
        passJson.put("formatVersion", 1);
        passJson.put("passTypeIdentifier", passTypeIdentifier);
        passJson.put("serialNumber", first.reservationId().toString());
        passJson.put("teamIdentifier", teamId);
        passJson.put("organizationName", "SeatReservation");
        passJson.put("description", first.eventName() != null ? first.eventName() : "Event Ticket");
        passJson.put("logoText", "SeatReservation");

        if (first.startTime() != null) {
            passJson.put("relevantDate", first.startTime().toString());
        }

        String qrPayload = buildQrCodePayload(first);
        passJson.put(
                "barcode",
                Map.of(
                        "format",
                        "PKBarcodeFormatQR",
                        "message",
                        qrPayload,
                        "messageEncoding",
                        "iso-8859-1"));

        Map<String, Object> eventTicket = new HashMap<>();

        if (first.eventName() != null) {
            eventTicket.put(
                    "primaryFields",
                    List.of(
                            Map.of(
                                    "key",
                                    "event",
                                    KEY_LABEL,
                                    "EVENT",
                                    KEY_VALUE,
                                    first.eventName())));
        }

        String combinedSeats =
                passes.stream()
                        .map(WalletPassData::seatLabel)
                        .filter(Objects::nonNull)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining(", "));

        eventTicket.put(
                "secondaryFields",
                List.of(
                        Map.of(
                                "key",
                                "location",
                                KEY_LABEL,
                                "LOCATION",
                                KEY_VALUE,
                                first.locationName() != null ? first.locationName() : ""),
                        Map.of(
                                "key",
                                "seat",
                                KEY_LABEL,
                                passes.size() > 1 ? "SEATS" : "SEAT",
                                KEY_VALUE,
                                !combinedSeats.isBlank()
                                        ? combinedSeats
                                        : (first.seatLabel() != null ? first.seatLabel() : ""))));

        List<Map<String, Object>> auxiliaryFields = new ArrayList<>();
        if (first.userName() != null && !first.userName().isBlank()) {
            auxiliaryFields.add(
                    Map.of("key", "name", KEY_LABEL, "GUEST", KEY_VALUE, first.userName()));
        }
        if (first.locationAddress() != null && !first.locationAddress().isBlank()) {
            auxiliaryFields.add(
                    Map.of(
                            "key",
                            "address",
                            KEY_LABEL,
                            "ADDRESS",
                            KEY_VALUE,
                            first.locationAddress()));
        }
        if (!auxiliaryFields.isEmpty()) {
            eventTicket.put("auxiliaryFields", auxiliaryFields);
        }

        List<Map<String, Object>> backFields = new ArrayList<>();
        if (first.eventDescription() != null && !first.eventDescription().isBlank()) {
            backFields.add(
                    Map.of(
                            "key",
                            "description",
                            KEY_LABEL,
                            "EVENT DETAILS",
                            KEY_VALUE,
                            first.eventDescription()));
        }
        if (first.locationAddress() != null && !first.locationAddress().isBlank()) {
            backFields.add(
                    Map.of(
                            "key",
                            "venue_address",
                            KEY_LABEL,
                            "VENUE ADDRESS",
                            KEY_VALUE,
                            first.locationAddress()));
        }
        if (first.startTime() != null) {
            backFields.add(
                    Map.of(
                            "key",
                            "start_time",
                            KEY_LABEL,
                            "START TIME",
                            KEY_VALUE,
                            first.startTime().toString()));
        }
        if (first.endTime() != null) {
            backFields.add(
                    Map.of(
                            "key",
                            "end_time",
                            KEY_LABEL,
                            "END TIME",
                            KEY_VALUE,
                            first.endTime().toString()));
        }
        if (passes.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < passes.size(); i++) {
                WalletPassData p = passes.get(i);
                if (p.seatLabel() != null && !p.seatLabel().isBlank()) {
                    sb.append(String.format("Seat %d: %s\n", i + 1, p.seatLabel()));
                }
            }
            if (!sb.isEmpty()) {
                backFields.add(
                        Map.of(
                                "key",
                                "seats_list",
                                KEY_LABEL,
                                "ALL SEATS",
                                KEY_VALUE,
                                sb.toString().trim()));
            }
        }
        if (!backFields.isEmpty()) {
            eventTicket.put("backFields", backFields);
        }

        passJson.put("eventTicket", eventTicket);
        return objectMapper.writeValueAsBytes(passJson);
    }

    @SuppressWarnings("java:S4790") // SHA-1 is mandated by Apple PassKit manifest specification
    protected byte[] buildManifest(Map<String, byte[]> files)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        HexFormat hex = HexFormat.of();
        Map<String, String> manifest = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            sha1.reset();
            manifest.put(entry.getKey(), hex.formatHex(sha1.digest(entry.getValue())));
        }
        return objectMapper.writeValueAsBytes(manifest);
    }

    protected byte[] buildZip(byte[] passJson, byte[] manifest, byte[] signature)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            writeEntry(zos, "pass.json", passJson);
            writeEntry(zos, "manifest.json", manifest);
            if (signature != null && signature.length > 0) {
                writeEntry(zos, "signature", signature);
            }
        }
        return baos.toByteArray();
    }

    protected byte[] buildPkpassesBundle(Map<String, byte[]> passFiles) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : passFiles.entrySet()) {
                writeEntry(zos, entry.getKey(), entry.getValue());
            }
        }
        return baos.toByteArray();
    }

    protected void writeEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    /** Visible for testing – returns the raw UTF-8 bytes of pass.json. */
    public byte[] buildPassJsonForTest(
            WalletPassData data, String passTypeIdentifier, String teamId) throws IOException {
        return buildPassJson(data, passTypeIdentifier, teamId);
    }
}
