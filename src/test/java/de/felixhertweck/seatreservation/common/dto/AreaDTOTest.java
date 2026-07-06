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
package de.felixhertweck.seatreservation.common.dto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import org.junit.jupiter.api.Test;

class AreaDTOTest {

    private Seat seat(String number, int x, int y, String area) {
        EventLocation location = new EventLocation();
        location.id = 1L;
        return new Seat(number, location, "Row", x, y, null, area);
    }

    @Test
    void fromSeats_returnsEmptyList_forNullInput() {
        assertTrue(AreaDTO.fromSeats(null).isEmpty());
    }

    @Test
    void fromSeats_ignoresSeatsWithoutArea() {
        List<Seat> seats =
                List.of(
                        seat("A1", 1, 1, null),
                        seat("A2", 2, 1, ""),
                        seat("A3", 3, 1, "   "),
                        seat("A4", 4, 1, "Balkon"));

        List<AreaDTO> areas = AreaDTO.fromSeats(seats);

        assertEquals(1, areas.size());
        assertEquals("Balkon", areas.get(0).name());
        assertEquals(1, areas.get(0).seats().size());
        assertEquals("A4", areas.get(0).seats().get(0).seatNumber());
    }

    @Test
    void fromSeats_groupsSeatsByAreaInFirstSeenOrder() {
        List<Seat> seats =
                List.of(
                        seat("A1", 1, 1, "Parkett"),
                        seat("B1", 1, 2, "Balkon"),
                        seat("A2", 2, 1, "Parkett"),
                        seat("B2", 2, 2, "Balkon"));

        List<AreaDTO> areas = AreaDTO.fromSeats(seats);

        assertEquals(2, areas.size());
        assertEquals("Parkett", areas.get(0).name());
        assertEquals(2, areas.get(0).seats().size());
        assertEquals("Balkon", areas.get(1).name());
        assertEquals(2, areas.get(1).seats().size());
    }
}
