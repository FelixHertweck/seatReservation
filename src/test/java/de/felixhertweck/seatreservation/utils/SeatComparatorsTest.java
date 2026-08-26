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
package de.felixhertweck.seatreservation.utils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import de.felixhertweck.seatreservation.email.service.SeatView;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import org.junit.jupiter.api.Test;

class SeatComparatorsTest {

    @Test
    void alphanumericComparator_NaturalOrderStringComparison() {
        var comp = SeatComparators.ALPHANUMERIC_COMPARATOR;

        // Equal strings
        assertEquals(0, comp.compare("A1", "A1"));
        assertEquals(0, comp.compare(null, null));
        assertEquals(0, comp.compare("", ""));

        // Null checks
        assertTrue(comp.compare(null, "A") < 0);
        assertTrue(comp.compare("A", null) > 0);

        // Numeric natural sorting
        assertTrue(comp.compare("A1", "A2") < 0);
        assertTrue(comp.compare("A2", "A10") < 0);
        assertTrue(comp.compare("A10", "A11") < 0);
        assertTrue(comp.compare("A9", "A10") < 0);
        assertTrue(comp.compare("A10", "B1") < 0);

        // Multiple numeric segments
        assertTrue(comp.compare("Row 1 Seat 2", "Row 1 Seat 10") < 0);
        assertTrue(comp.compare("Row 2 Seat 1", "Row 10 Seat 1") < 0);

        // Numbers with leading zeros
        assertTrue(comp.compare("A1", "A01") < 0);
    }

    @Test
    void alphanumericComparator_SortList() {
        List<String> list = new ArrayList<>(List.of("A10", "A1", "B2", "A2", "B1", "A100", "A20"));
        list.sort(SeatComparators.ALPHANUMERIC_COMPARATOR);

        assertEquals(List.of("A1", "A2", "A10", "A20", "A100", "B1", "B2"), list);
    }

    @Test
    void seatComparator_SortsByRowThenNumber() {
        Seat s1 = new Seat("1", "Row 1", null);
        Seat s2 = new Seat("10", "Row 1", null);
        Seat s3 = new Seat("2", "Row 1", null);
        Seat s4 = new Seat("1", "Row 2", null);
        Seat s5 = new Seat("1", "Row 10", null);

        List<Seat> seats = new ArrayList<>(List.of(s5, s4, s2, s1, s3));
        seats.sort(SeatComparators.SEAT_COMPARATOR);

        assertEquals(List.of(s1, s3, s2, s4, s5), seats);
    }

    @Test
    void seatViewComparator_SortsByRowThenNumber() {
        SeatView sv1 = new SeatView("1", "Row 1", "Balcony");
        SeatView sv2 = new SeatView("10", "Row 1", "Balcony");
        SeatView sv3 = new SeatView("2", "Row 1", "Balcony");
        SeatView sv4 = new SeatView("1", "Row 2", "Parquet");

        List<SeatView> seatViews = new ArrayList<>(List.of(sv2, sv4, sv1, sv3));
        seatViews.sort(SeatComparators.SEAT_VIEW_COMPARATOR);

        assertEquals(List.of(sv1, sv3, sv2, sv4), seatViews);
    }

    @Test
    void reservationComparator_SortsBySeat() {
        User user = new User();
        Event event = new Event();

        Seat s1 = new Seat("A1", "1", null);
        Seat s2 = new Seat("A2", "1", null);
        Seat s3 = new Seat("A10", "1", null);

        Reservation r1 = new Reservation(user, event, s1, null, null, null);
        Reservation r2 = new Reservation(user, event, s2, null, null, null);
        Reservation r3 = new Reservation(user, event, s3, null, null, null);

        List<Reservation> reservations = new ArrayList<>(List.of(r3, r1, r2));
        reservations.sort(SeatComparators.RESERVATION_COMPARATOR);

        assertEquals(List.of(r1, r2, r3), reservations);
    }
}
