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

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import org.junit.jupiter.api.Test;

class ReservationExporterTest {

    private Reservation createReservation(
            long id,
            String seatNumber,
            String seatRow,
            String firstName,
            String lastName,
            ReservationStatus status) {
        User user = new User();
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.id = id;
        Seat seat = new Seat();
        seat.id = id;
        seat.setSeatNumber(seatNumber);
        seat.setSeatRow(seatRow);
        Reservation reservation = new Reservation();
        reservation.id = id;
        reservation.setUser(user);
        reservation.setSeat(seat);
        reservation.setReservationDate(Instant.now());
        reservation.setStatus(status);
        return reservation;
    }

    @Test
    void exportReservationsToPdf_createsNonEmptyPdf() throws IOException {
        Reservation reservation =
                createReservation(1L, "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        byte[] pdfBytes =
                ReservationExporter.exportReservationsToPdf(List.of(reservation), "31.12.2025")
                        .toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100, "PDF should not be empty");
        // Check PDF header
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    void exportReservationsToPdf_multipleReservations_createsMultiPagePdf() throws IOException {
        Reservation r1 =
                createReservation(1L, "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        Reservation r2 =
                createReservation(2L, "B2", "2", "Erika", "Musterfrau", ReservationStatus.RESERVED);
        byte[] pdfBytes =
                ReservationExporter.exportReservationsToPdf(List.of(r1, r2), "01.01.2026")
                        .toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100, "PDF should not be empty");
        // Check PDF header
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    void exportReservationsToPdf_emptyList_createsValidPdf() throws IOException {
        byte[] pdfBytes =
                ReservationExporter.exportReservationsToPdf(Collections.emptyList(), "01.01.2026")
                        .toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    void exportReservationsToCsv_createsCsvWithHeaderAndRows() throws IOException {
        Reservation reservation =
                createReservation(1L, "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(reservation)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(
                csv.startsWith(
                        "ID,Reservation Status,Seat Number,Seat Row,First Name,Last"
                                + " Name,Reservation Date"));
        assertTrue(csv.contains("A1"));
        assertTrue(csv.contains("Max"));
    }

    @Test
    void exportReservationsToCsv_emptyList_createsCsvWithHeaderOnly() throws IOException {
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(Collections.emptyList()).toByteArray();
        String csv = new String(csvBytes);
        assertEquals(
                "ID,Reservation Status,Seat Number,Seat Row,First Name,Last Name,Reservation"
                        + " Date\n",
                csv);
    }

    @Test
    void exportReservationsToCsv_multipleReservations_createsCsvWithMultipleRows()
            throws IOException {
        Reservation r1 =
                createReservation(1L, "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        Reservation r2 =
                createReservation(2L, "B2", "2", "Erika", "Musterfrau", ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(r1, r2)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(
                csv.startsWith(
                        "ID,Reservation Status,Seat Number,Seat Row,First Name,Last"
                                + " Name,Reservation Date"));
        assertTrue(csv.contains("Max"));
        assertTrue(csv.contains("Erika"));
        long linebreaks = csv.chars().filter(ch -> ch == '\n').count();
        assertEquals(3, linebreaks, "CSV should have one header row and two data rows");
    }

    @Test
    void exportReservationsToPdf_withBlockedReservation_createsValidPdf() throws IOException {
        Reservation r1 = createReservation(1L, "C1", "3", null, null, ReservationStatus.BLOCKED);
        byte[] pdfBytes =
                ReservationExporter.exportReservationsToPdf(List.of(r1), null).toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100, "PDF should not be empty");
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    void exportReservationsToPdf_withMixedStatus_createsValidPdf() throws IOException {
        Reservation r1 =
                createReservation(1L, "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        Reservation r2 = createReservation(2L, "C1", "3", null, null, ReservationStatus.BLOCKED);
        byte[] pdfBytes =
                ReservationExporter.exportReservationsToPdf(List.of(r1, r2), "01.01.2026")
                        .toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100, "PDF should not be empty");
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }
}
