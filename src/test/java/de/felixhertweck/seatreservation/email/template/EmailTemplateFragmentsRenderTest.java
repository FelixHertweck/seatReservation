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
package de.felixhertweck.seatreservation.email.template;

import java.util.List;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.email.service.SeatView;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Renders the email templates that were split into shared {@code email/fragments/*} includes
 * (styles, QR code block, entrance-info block) to guard against a broken include (e.g. a stray
 * unrendered {@code {#include ...}} tag) or a missing style rule slipping through unnoticed.
 */
@QuarkusTest
class EmailTemplateFragmentsRenderTest {

    @Inject
    @Location("email/reservation-confirmation")
    Template reservationConfirmation;

    @Inject
    @Location("email/reservation-update-confirmation")
    Template reservationUpdateConfirmation;

    @Inject
    @Location("email/event-reminder")
    Template eventReminder;

    @Inject
    @Location("email/manager-reservation-export")
    Template managerReservationExport;

    private static void assertFragmentsRendered(String html) {
        assertFalse(html.contains("{#include"), "an include tag was left unrendered");
        assertTrue(html.contains(".button {"), "action-styles fragment did not render");
        assertTrue(html.contains(".seatmap-container {"), "action-styles fragment did not render");
    }

    @Test
    void reservationConfirmation_rendersIncludedFragments() {
        String html =
                reservationConfirmation
                        .data("userName", "jane")
                        .data("fullName", "Jane Doe")
                        .data("eventName", "Concert")
                        .data("eventLocation", "Main Hall")
                        .data("eventStartTime", "2026-07-10 20:00")
                        .data("eventEndTime", "2026-07-10 23:00")
                        .data("newSeats", List.of(new SeatView("A1", "1", "Parkett")))
                        .data("hasExistingSeats", false)
                        .data("existingSeats", List.of())
                        .data("entranceInfo", "Use the north entrance")
                        .data("eventLink", "https://example.com/e")
                        .data("seatmapLink", "https://example.com/s")
                        .data("currentYear", "2026")
                        .render();

        assertFragmentsRendered(html);
        assertTrue(html.contains(".header {"), "simple-card-styles fragment did not render");
        assertTrue(html.contains("Your Check-in QR Code"), "qrcode-block fragment did not render");
        assertTrue(html.contains("Entrance Information"), "entrance-info-block did not render");
        assertTrue(html.contains("Use the north entrance"));
        assertTrue(html.contains("<li>A1 (1) - Parkett</li>"));
    }

    @Test
    void reservationUpdateConfirmation_rendersIncludedFragments() {
        String html =
                reservationUpdateConfirmation
                        .data("userName", "jane")
                        .data("fullName", "Jane Doe")
                        .data("eventName", "Concert")
                        .data("eventLocation", "Main Hall")
                        .data("eventStartTime", "2026-07-10 20:00")
                        .data("eventEndTime", "2026-07-10 23:00")
                        .data("deletedSeats", List.of(new SeatView("A1", "1", "Parkett")))
                        .data("hasActiveSeats", true)
                        .data("activeSeats", List.of(new SeatView("A2", "1", "Parkett")))
                        .data("entranceInfo", "Use the north entrance")
                        .data("eventLink", "https://example.com/e")
                        .data("seatmapLink", "https://example.com/s")
                        .data("currentYear", "2026")
                        .render();

        assertFragmentsRendered(html);
        assertTrue(html.contains(".header {"), "simple-card-styles fragment did not render");
        assertTrue(html.contains("Your Check-in QR Code"), "qrcode-block fragment did not render");
        assertTrue(html.contains("Entrance Information"), "entrance-info-block did not render");
        assertTrue(html.contains("<li>A1 (1) - Parkett</li>"));
        assertTrue(html.contains("<li>A2 (1) - Parkett</li>"));
    }

    @Test
    void eventReminder_rendersIncludedFragments() {
        String html =
                eventReminder
                        .data("userName", "jane")
                        .data("fullName", "Jane Doe")
                        .data("eventName", "Concert")
                        .data("eventDate", "2026-07-10")
                        .data("eventTime", "20:00")
                        .data("eventLocation", "Main Hall")
                        .data("seats", List.of(new SeatView("A1", "1", "Parkett")))
                        .data("entranceInfo", "Use the north entrance")
                        .data("eventLink", "https://example.com/e")
                        .data("seatmapLink", "https://example.com/s")
                        .data("currentYear", "2026")
                        .render();

        assertFragmentsRendered(html);
        assertTrue(html.contains("h1 {"), "card-layout-styles fragment did not render");
        assertTrue(html.contains("Your Check-in QR Code"), "qrcode-block fragment did not render");
        assertTrue(html.contains("Entrance Information"), "entrance-info-block did not render");
        assertTrue(html.contains("<li>A1 (1) - Parkett</li>"));
    }

    @Test
    void eventReminder_seatWithoutArea_rendersWithoutTrailingDash() {
        String html =
                eventReminder
                        .data("userName", "jane")
                        .data("fullName", "Jane Doe")
                        .data("eventName", "Concert")
                        .data("eventDate", "2026-07-10")
                        .data("eventTime", "20:00")
                        .data("eventLocation", "Main Hall")
                        .data("seats", List.of(new SeatView("A1", "1", null)))
                        .data("entranceInfo", "Use the north entrance")
                        .data("eventLink", "https://example.com/e")
                        .data("seatmapLink", "https://example.com/s")
                        .data("currentYear", "2026")
                        .render();

        assertTrue(html.contains("<li>A1 (1)</li>"));
        assertFalse(html.contains("A1 (1) -"));
    }

    @Test
    void managerReservationExport_rendersIncludedFragments() {
        String html =
                managerReservationExport
                        .data("fullName", "Jane Doe")
                        .data("eventName", "Concert")
                        .data("eventDate", "2026-07-10")
                        .data("eventTime", "20:00")
                        .data("eventLocation", "Main Hall")
                        .data("currentYear", "2026")
                        .render();

        assertFalse(html.contains("{#include"), "an include tag was left unrendered");
        assertTrue(html.contains("h1 {"), "card-layout-styles fragment did not render");
    }
}
