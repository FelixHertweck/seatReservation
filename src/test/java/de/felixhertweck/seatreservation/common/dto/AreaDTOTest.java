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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.Coordinate;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.Seat;
import org.junit.jupiter.api.Test;

class AreaDTOTest {

    private EventLocationArea area(long id, String name) {
        EventLocationArea area = new EventLocationArea(name);
        area.id = id;
        return area;
    }

    private Seat seat(long id, String number, int x, int y, EventLocationArea area) {
        EventLocation location = new EventLocation();
        location.id = 1L;
        Seat seat = new Seat(number, location, "Row", x, y, null, area);
        seat.id = id;
        return seat;
    }

    @Test
    void fromAreas_returnsEmptyList_forNullInput() {
        assertTrue(AreaDTO.fromAreas(null).isEmpty());
    }

    @Test
    void fromAreas_ignoresSeatsWithoutArea() {
        EventLocationArea balkon = area(1, "Balkon");
        List<Seat> seats =
                List.of(
                        seat(1, "A1", 1, 1, null),
                        seat(2, "A2", 2, 1, null),
                        seat(3, "A3", 3, 1, null),
                        seat(4, "A4", 4, 1, balkon));

        List<AreaDTO> areas = AreaDTO.fromAreas(seats);

        assertEquals(1, areas.size());
        assertEquals("Balkon", areas.get(0).name());
        assertEquals(1, areas.get(0).seatIds().size());
        assertEquals(4L, areas.get(0).seatIds().get(0));
    }

    @Test
    void fromAreas_groupsSeatsByAreaInFirstSeenOrder() {
        EventLocationArea parkett = area(1, "Parkett");
        EventLocationArea balkon = area(2, "Balkon");
        List<Seat> seats =
                List.of(
                        seat(1, "A1", 1, 1, parkett),
                        seat(2, "B1", 1, 2, balkon),
                        seat(3, "A2", 2, 1, parkett),
                        seat(4, "B2", 2, 2, balkon));

        List<AreaDTO> areas = AreaDTO.fromAreas(seats);

        assertEquals(2, areas.size());
        assertEquals("Parkett", areas.get(0).name());
        assertEquals(2, areas.get(0).seatIds().size());
        assertEquals("Balkon", areas.get(1).name());
        assertEquals(2, areas.get(1).seatIds().size());
    }

    @Test
    void fromAreas_leavesBoundaryNull_whenAreaHasNoBoundary() {
        EventLocationArea parkett = area(1, "Parkett");
        List<Seat> seats = List.of(seat(1, "A1", 1, 1, parkett));

        List<AreaDTO> areas = AreaDTO.fromAreas(seats);

        assertEquals(1, areas.size());
        assertNull(areas.get(0).boundary());
    }

    @Test
    void fromAreas_attachesBoundary_whenAreaHasOne() {
        EventLocationArea parkett = area(1, "Parkett");
        parkett.setBoundary(
                List.of(
                        new Coordinate(1, 1),
                        new Coordinate(2, 1),
                        new Coordinate(2, 2),
                        new Coordinate(1, 2)));
        EventLocationArea balkon = area(2, "Balkon");
        List<Seat> seats =
                List.of(
                        seat(1, "A1", 1, 1, parkett),
                        seat(2, "A2", 2, 1, parkett),
                        seat(3, "B1", 1, 2, balkon));

        List<AreaDTO> areas = AreaDTO.fromAreas(seats);

        assertEquals(2, areas.size());
        assertEquals("Parkett", areas.get(0).name());
        assertEquals(
                List.of(
                        new CoordinateDTO(1, 1),
                        new CoordinateDTO(2, 1),
                        new CoordinateDTO(2, 2),
                        new CoordinateDTO(1, 2)),
                areas.get(0).boundary());
        assertEquals("Balkon", areas.get(1).name());
        assertNull(areas.get(1).boundary());
    }
}
