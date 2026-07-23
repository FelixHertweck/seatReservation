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
package de.felixhertweck.seatreservation.model.entity;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SeatTest {

    @Test
    void equals_persistedSeatWithMatchingFields_isNotEqualToTransientSeat() {
        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat persisted =
                new Seat(
                        "A1",
                        location,
                        "Row 1",
                        1,
                        1,
                        new EventLocationEntrance("Entrance A"),
                        new EventLocationArea("Parkett"));
        persisted.id = id(5);

        Seat transientSeat =
                new Seat(
                        "A1",
                        location,
                        "Row 1",
                        1,
                        1,
                        new EventLocationEntrance("Entrance A"),
                        new EventLocationArea("Parkett"));

        assertNotEquals(persisted, transientSeat);
        assertNotEquals(transientSeat, persisted);
    }

    @Test
    void equals_sameId_isEqualRegardlessOfOtherFields() {
        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat a =
                new Seat(
                        "A1",
                        location,
                        "Row 1",
                        1,
                        1,
                        new EventLocationEntrance("Entrance A"),
                        new EventLocationArea("Parkett"));
        a.id = id(5);
        Seat b =
                new Seat(
                        "B2",
                        location,
                        "Row 2",
                        2,
                        2,
                        new EventLocationEntrance("Entrance B"),
                        new EventLocationArea("Balkon"));
        b.id = id(5);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_bothTransientWithSameFields_isEqual() {
        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat a =
                new Seat(
                        "A1",
                        location,
                        "Row 1",
                        1,
                        1,
                        new EventLocationEntrance("Entrance A"),
                        new EventLocationArea("Parkett"));
        Seat b =
                new Seat(
                        "A1",
                        location,
                        "Row 1",
                        1,
                        1,
                        new EventLocationEntrance("Entrance A"),
                        new EventLocationArea("Parkett"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void hashCode_consistentWithEquals_forDifferentIds() {
        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat a =
                new Seat(
                        "A1",
                        location,
                        "Row 1",
                        1,
                        1,
                        new EventLocationEntrance("Entrance A"),
                        new EventLocationArea("Parkett"));
        a.id = id(5);
        Seat b =
                new Seat(
                        "A1",
                        location,
                        "Row 1",
                        1,
                        1,
                        new EventLocationEntrance("Entrance A"),
                        new EventLocationArea("Parkett"));
        b.id = id(6);

        assertNotEquals(a, b);
    }
}
