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

import java.util.Comparator;
import java.util.Objects;

import de.felixhertweck.seatreservation.email.service.SeatView;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;

/**
 * Utility comparators for natural (alphanumeric) sorting of seats, seat views, and reservations.
 */
public final class SeatComparators {

    private SeatComparators() {}

    /**
     * Natural / alphanumeric string comparator. Treats digit sequences within strings as numbers
     * (e.g. "A2" &lt; "A10", "Row 1" &lt; "Row 2" &lt; "Row 10").
     */
    public static final Comparator<String> ALPHANUMERIC_COMPARATOR =
            (s1, s2) -> {
                if (Objects.equals(s1, s2)) return 0;
                if (s1 == null) return -1;
                if (s2 == null) return 1;

                int i1 = 0;
                int i2 = 0;
                int len1 = s1.length();
                int len2 = s2.length();

                while (i1 < len1 && i2 < len2) {
                    char c1 = s1.charAt(i1);
                    char c2 = s2.charAt(i2);

                    if (Character.isDigit(c1) && Character.isDigit(c2)) {
                        int start1 = i1;
                        while (i1 < len1 && Character.isDigit(s1.charAt(i1))) {
                            i1++;
                        }
                        int start2 = i2;
                        while (i2 < len2 && Character.isDigit(s2.charAt(i2))) {
                            i2++;
                        }

                        String num1 = s1.substring(start1, i1);
                        String num2 = s2.substring(start2, i2);

                        String trim1 = num1.replaceFirst("^0+(?!$)", "");
                        String trim2 = num2.replaceFirst("^0+(?!$)", "");

                        if (trim1.length() != trim2.length()) {
                            return Integer.compare(trim1.length(), trim2.length());
                        }
                        int cmp = trim1.compareTo(trim2);
                        if (cmp != 0) {
                            return cmp;
                        }
                        int zeroCmp = Integer.compare(num1.length(), num2.length());
                        if (zeroCmp != 0) {
                            return zeroCmp;
                        }
                    } else {
                        int cmp =
                                Character.compare(
                                        Character.toLowerCase(c1), Character.toLowerCase(c2));
                        if (cmp != 0) {
                            return cmp;
                        }
                        i1++;
                        i2++;
                    }
                }

                return Integer.compare(len1 - i1, len2 - i2);
            };

    /**
     * Compares two seats by row (alphanumeric/natural order), then by seat number
     * (alphanumeric/natural order).
     */
    public static final Comparator<Seat> SEAT_COMPARATOR =
            (s1, s2) -> {
                if (Objects.equals(s1, s2)) return 0;
                if (s1 == null) return -1;
                if (s2 == null) return 1;

                String r1 = s1.getSeatRow() != null ? s1.getSeatRow() : "";
                String r2 = s2.getSeatRow() != null ? s2.getSeatRow() : "";
                int rowComp = ALPHANUMERIC_COMPARATOR.compare(r1, r2);
                if (rowComp != 0) return rowComp;

                String n1 = s1.getSeatNumber() != null ? s1.getSeatNumber() : "";
                String n2 = s2.getSeatNumber() != null ? s2.getSeatNumber() : "";
                return ALPHANUMERIC_COMPARATOR.compare(n1, n2);
            };

    /**
     * Compares two SeatView objects by row (alphanumeric/natural order), then by seat number
     * (alphanumeric/natural order).
     */
    public static final Comparator<SeatView> SEAT_VIEW_COMPARATOR =
            (s1, s2) -> {
                if (Objects.equals(s1, s2)) return 0;
                if (s1 == null) return -1;
                if (s2 == null) return 1;

                String r1 = s1.row() != null ? s1.row() : "";
                String r2 = s2.row() != null ? s2.row() : "";
                int rowComp = ALPHANUMERIC_COMPARATOR.compare(r1, r2);
                if (rowComp != 0) return rowComp;

                String n1 = s1.number() != null ? s1.number() : "";
                String n2 = s2.number() != null ? s2.number() : "";
                return ALPHANUMERIC_COMPARATOR.compare(n1, n2);
            };

    /** Compares two reservations by their assigned seats. */
    public static final Comparator<Reservation> RESERVATION_COMPARATOR =
            (r1, r2) -> {
                if (Objects.equals(r1, r2)) return 0;
                if (r1 == null) return -1;
                if (r2 == null) return 1;

                Seat s1 = r1.getSeat();
                Seat s2 = r2.getSeat();
                return SEAT_COMPARATOR.compare(s1, s2);
            };
}
