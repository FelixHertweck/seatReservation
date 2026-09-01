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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.TextField;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReservationExporterTest {

    @TempDir Path tempDir;

    private Reservation createReservation(
            UUID id,
            String seatNumber,
            String seatRow,
            String firstName,
            String lastName,
            ReservationStatus status) {
        User user = new User();
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.id = id;
        Seat seat = new Seat(seatNumber, seatRow, null);
        seat.id = id;
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
                createReservation(
                        id(1), "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
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
                createReservation(
                        id(1), "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        Reservation r2 =
                createReservation(
                        id(2), "B2", "2", "Erika", "Musterfrau", ReservationStatus.RESERVED);
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
                createReservation(
                        id(1), "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(reservation)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(
                csv.startsWith(
                        "ID,Reservation Status,Seat Number,Seat Row,Entrance,Area,First Name,Last"
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
                "ID,Reservation Status,Seat Number,Seat Row,Entrance,Area,First Name,Last"
                        + " Name,Reservation Date\r\n",
                csv);
    }

    @Test
    void exportReservationsToCsv_multipleReservations_createsCsvWithMultipleRows()
            throws IOException {
        Reservation r1 =
                createReservation(
                        id(1), "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        Reservation r2 =
                createReservation(
                        id(2), "B2", "2", "Erika", "Musterfrau", ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(r1, r2)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(
                csv.startsWith(
                        "ID,Reservation Status,Seat Number,Seat Row,Entrance,Area,First Name,Last"
                                + " Name,Reservation Date"));
        assertTrue(csv.contains("Max"));
        assertTrue(csv.contains("Erika"));
        long linebreaks = csv.chars().filter(ch -> ch == '\n').count();
        assertEquals(3, linebreaks, "CSV should have one header row and two data rows");
    }

    @Test
    void exportReservationsToCsv_fieldStartingWithFormulaChar_isEscapedWithLeadingApostrophe()
            throws IOException {
        Reservation reservation =
                createReservation(
                        id(1),
                        "A1",
                        "1",
                        "=cmd|'/C calc'!A0",
                        "Mustermann",
                        ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(reservation)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(csv.contains("'=cmd|'/C calc'!A0"), csv);
    }

    @Test
    void exportReservationsToCsv_fieldWithLeadingNonBreakingSpace_isEscapedWithLeadingApostrophe()
            throws IOException {
        // U+00A0 (NO-BREAK SPACE) is trimmed by Excel/LibreOffice before evaluating a leading
        // formula-trigger character, so it must be treated like ordinary whitespace here.
        Reservation reservation =
                createReservation(
                        id(1),
                        "A1",
                        "1",
                        " =cmd|'/C calc'!A0",
                        "Mustermann",
                        ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(reservation)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(csv.contains("' =cmd|'/C calc'!A0"), csv);
    }

    @Test
    void exportReservationsToCsv_fieldWithLeadingCarriageReturnAfterWhitespace_isEscaped()
            throws IOException {
        Reservation reservation =
                createReservation(
                        id(1), "A1", "1", " \r=cmd", "Mustermann", ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(reservation)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(csv.contains("' \r=cmd"), csv);
    }

    @Test
    void exportReservationsToCsv_fieldWithLeadingTabAfterWhitespace_isEscaped() throws IOException {
        Reservation reservation =
                createReservation(
                        id(1), "A1", "1", " \n\tcmd", "Mustermann", ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(reservation)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(csv.contains("' \n\tcmd"), csv);
    }

    @Test
    void exportReservationsToCsv_fieldWithoutFormulaTrigger_isNotEscaped() throws IOException {
        Reservation reservation =
                createReservation(
                        id(1), "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(reservation)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(csv.contains(",Max,"), csv);
        assertTrue(!csv.contains("'Max"), csv);
    }

    @Test
    void exportReservationsToCsv_fieldWithComma_isQuoted() throws IOException {
        Reservation reservation =
                createReservation(
                        id(1), "A1", "1", "Max,Junior", "Mustermann", ReservationStatus.RESERVED);
        byte[] csvBytes =
                ReservationExporter.exportReservationsToCsv(List.of(reservation)).toByteArray();
        String csv = new String(csvBytes);
        assertTrue(csv.contains("\"Max,Junior\""), csv);
    }

    @Test
    void exportReservationsToPdf_withBlockedReservation_createsValidPdf() throws IOException {
        Reservation r1 = createReservation(id(1), "C1", "3", null, null, ReservationStatus.BLOCKED);
        byte[] pdfBytes =
                ReservationExporter.exportReservationsToPdf(List.of(r1), null).toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100, "PDF should not be empty");
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @AfterEach
    void clearOverrideDirProperty() {
        System.clearProperty("template.override-dir");
    }

    private byte[] buildOverrideBlockedTemplate() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Document document = new Document()) {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();
            PdfContentByte canvas = writer.getDirectContent();
            ColumnText.showTextAligned(
                    canvas, Element.ALIGN_LEFT, new Phrase("OVERRIDE MARKER"), 50, 700, 0);
            TextField seatInfoField =
                    new TextField(writer, new Rectangle(50, 600, 250, 620), "seatInfo");
            writer.addAnnotation(seatInfoField.getTextField());
        }
        return baos.toByteArray();
    }

    @Test
    void exportReservationsToPdf_withOverrideDirTemplate_usesExternalTemplate() throws Exception {
        Path exportDir = tempDir.resolve("export");
        Files.createDirectories(exportDir);
        Files.write(exportDir.resolve("blocked.pdf"), buildOverrideBlockedTemplate());
        System.setProperty("template.override-dir", tempDir.toString());

        Reservation reservation =
                createReservation(id(1), "C1", "3", null, null, ReservationStatus.BLOCKED);
        byte[] pdfBytes =
                ReservationExporter.exportReservationsToPdf(List.of(reservation), null)
                        .toByteArray();

        String text = new PdfTextExtractor(new PdfReader(pdfBytes)).getTextFromPage(1);
        assertTrue(text.contains("OVERRIDE MARKER"), text);
        assertTrue(text.contains("C1 (3)"), text);
    }

    @Test
    void exportReservationsToPdf_withMixedStatus_createsValidPdf() throws IOException {
        Reservation r1 =
                createReservation(
                        id(1), "A1", "1", "Max", "Mustermann", ReservationStatus.RESERVED);
        Reservation r2 = createReservation(id(2), "C1", "3", null, null, ReservationStatus.BLOCKED);
        byte[] pdfBytes =
                ReservationExporter.exportReservationsToPdf(List.of(r1, r2), "01.01.2026")
                        .toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100, "PDF should not be empty");
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }
}
