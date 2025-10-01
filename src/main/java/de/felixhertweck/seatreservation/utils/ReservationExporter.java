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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.User;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.AcroFields;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfImportedPage;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.PdfWriter;

public class ReservationExporter {

    private static final String templatePathReserved = "/export-template/reserved.pdf";
    private static final String templatePathBlocked = "/export-template/blocked.pdf";

    /**
     * Exports a list of reservations to a CSV format.
     *
     * @param reservations the list of reservations to export
     * @return a ByteArrayOutputStream containing the CSV data
     * @throws IOException if an I/O error occurs
     */
    public static ByteArrayOutputStream exportReservationsToCsv(Iterable<Reservation> reservations)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // CSV Header
            writer.write("ID,Seat Number,First Name,Last Name,Reservation Date\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            for (Reservation reservation : reservations) {
                writer.write(
                        String.format(
                                "%d,%s,%s,%s,%s,%s\n",
                                reservation.id,
                                reservation.getStatus(),
                                reservation.getSeat().getSeatNumber(),
                                reservation.getSeat().getSeatRow(),
                                reservation.getUser().getFirstname(),
                                reservation.getUser().getLastname(),
                                reservation
                                        .getReservationDate()
                                        .atZone(ZoneId.systemDefault())
                                        .format(formatter)));
            }
            writer.flush();
        }
        return baos;
    }

    /**
     * Exports a list of reservations as a PDF. Each reservation gets its own page. If a template
     * PDF with form fields is configured, it will be used and filled. Otherwise, a standard layout
     * with centered text will be created.
     *
     * @param reservations the reservations to export
     * @param reservedUntilValue the "reserved until" value
     * @return a ByteArrayOutputStream containing the PDF data
     * @throws IOException if an I/O error occurs
     */
    public static ByteArrayOutputStream exportReservationsToPdf(
            List<Reservation> reservations, String reservedUntilValue) throws IOException {
        try (ByteArrayOutputStream finalBaos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, finalBaos);
            document.open();

            if (reservations.isEmpty()) {
                addNoReservationsPage(writer, document);
            } else {
                for (Reservation reservation : reservations) {
                    if (reservation.getStatus() == ReservationStatus.BLOCKED) {
                        byte[] templateBytes = loadTemplatePdf(templatePathBlocked);
                        if (templateBytes != null) {
                            addPageFromTemplate(writer, document, reservation, null, templateBytes);
                        } else {
                            addStandardBlockedPage(writer, document, reservation);
                        }
                    } else { // RESERVED or other statuses
                        byte[] templateBytes = loadTemplatePdf(templatePathReserved);
                        if (templateBytes != null) {
                            addPageFromTemplate(
                                    writer,
                                    document,
                                    reservation,
                                    reservedUntilValue,
                                    templateBytes);
                        } else {
                            addStandardReservedPage(
                                    writer, document, reservation, reservedUntilValue);
                        }
                    }
                }
            }

            document.close();
            return finalBaos;
        } catch (DocumentException e) {
            throw new IOException("Error creating PDF document", e);
        }
    }

    private static byte[] loadTemplatePdf(String path) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        InputStream resourceStream = ReservationExporter.class.getResourceAsStream(path);
        if (resourceStream != null) {
            try (InputStream is = resourceStream) {
                return is.readAllBytes();
            }
        }
        File templateFile = new File(path);
        if (templateFile.exists() && templateFile.canRead()) {
            return Files.readAllBytes(templateFile.toPath());
        }
        return null;
    }

    private static void addPageFromTemplate(
            PdfWriter writer,
            Document document,
            Reservation reservation,
            String reservedUntilValue,
            byte[] templatePdfBytes)
            throws IOException, DocumentException {
        try (ByteArrayOutputStream tempBaos = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(templatePdfBytes);
            PdfStamper stamper = new PdfStamper(reader, tempBaos);
            AcroFields form = stamper.getAcroFields();

            if (reservation.getStatus() == ReservationStatus.BLOCKED) {
                String seatInfo =
                        String.format(
                                "%s (%s)",
                                reservation.getSeat().getSeatNumber(),
                                reservation.getSeat().getSeatRow());
                form.setField("seatInfo", seatInfo);
            } else {
                if (reservedUntilValue != null) {
                    form.setField("reservedUntil", reservedUntilValue);
                }
                User user = reservation.getUser();
                String userName = user.getFirstname() + " " + user.getLastname();
                form.setField("userName", userName);
                String seatInfo =
                        String.format(
                                "%s (%s)",
                                reservation.getSeat().getSeatNumber(),
                                reservation.getSeat().getSeatRow());
                form.setField("seatInfo", seatInfo);
            }

            stamper.setFormFlattening(true);
            stamper.close();

            byte[] filledPageBytes = tempBaos.toByteArray();
            PdfReader filledReader = new PdfReader(filledPageBytes);
            document.setPageSize(filledReader.getPageSizeWithRotation(1));
            document.newPage();
            PdfImportedPage importedPage = writer.getImportedPage(filledReader, 1);
            writer.getDirectContentUnder().addTemplate(importedPage, 0, 0);
            filledReader.close();
            reader.close();
        }
    }

    private static void addStandardBlockedPage(
            PdfWriter writer, Document document, Reservation reservation) {
        document.setPageSize(PageSize.A4.rotate());
        document.newPage();
        PdfContentByte canvas = writer.getDirectContent();
        float centerX = document.getPageSize().getWidth() / 2;
        float pageHeight = document.getPageSize().getHeight();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
        Font seatFont = FontFactory.getFont(FontFactory.HELVETICA, 16);

        Phrase blockedText = new Phrase("Blocked", titleFont);
        ColumnText.showTextAligned(
                canvas, Element.ALIGN_CENTER, blockedText, centerX, pageHeight - 100, 0);

        String seatInfo =
                String.format(
                        "%s (%s)",
                        reservation.getSeat().getSeatNumber(), reservation.getSeat().getSeatRow());
        Phrase seat = new Phrase(seatInfo, seatFont);
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, seat, centerX, pageHeight / 2, 0);
    }

    private static void addStandardReservedPage(
            PdfWriter writer,
            Document document,
            Reservation reservation,
            String reservedUntilValue) {
        document.setPageSize(PageSize.A4.rotate());
        document.newPage();
        PdfContentByte canvas = writer.getDirectContent();
        float centerX = document.getPageSize().getWidth() / 2;
        float pageHeight = document.getPageSize().getHeight();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 40);
        Font seatFont = FontFactory.getFont(FontFactory.HELVETICA, 16);
        Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 14);

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase("Reserved", titleFont),
                centerX,
                pageHeight - 100,
                0);

        if (reservedUntilValue != null && !reservedUntilValue.trim().isEmpty()) {
            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_CENTER,
                    new Phrase("until", dateFont),
                    centerX,
                    pageHeight - 140,
                    0);
            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_CENTER,
                    new Phrase(reservedUntilValue, dateFont),
                    centerX,
                    pageHeight - 170,
                    0);
        }

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase("for", titleFont),
                centerX,
                pageHeight / 2 + 50,
                0);

        User user = reservation.getUser();
        String userName = user.getFirstname() + " " + user.getLastname();
        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase(userName, nameFont),
                centerX,
                pageHeight / 2,
                0);

        String seatInfo =
                String.format(
                        "%s (%s)",
                        reservation.getSeat().getSeatNumber(), reservation.getSeat().getSeatRow());
        ColumnText.showTextAligned(
                canvas, Element.ALIGN_CENTER, new Phrase(seatInfo, seatFont), centerX, 100, 0);
    }

    private static void addNoReservationsPage(PdfWriter writer, Document document) {
        document.newPage();
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 16);
        Phrase phrase = new Phrase("No reservations found.", font);
        ColumnText.showTextAligned(
                writer.getDirectContent(),
                Element.ALIGN_CENTER,
                phrase,
                document.getPageSize().getWidth() / 2,
                document.getPageSize().getHeight() / 2,
                0);
    }
}
