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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.Coordinate;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.Seat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AreaDTOTest {

    private EventLocation location;

    @BeforeEach
    void setUp() {
        location = new EventLocation();
        location.id = id(1);
    }

    private EventLocationArea area(long id, String name) {
        EventLocationArea area = new EventLocationArea(name);
        area.id = id(id);
        area.setEventLocation(location);
        location.getAreas().add(area);
        return area;
    }

    private Seat seat(long id, String number, int x, int y, EventLocationArea area) {
        Seat seat = new Seat(number, location, "Row", x, y, null, area);
        seat.id = id(id);
        location.getSeats().add(seat);
        return seat;
    }

    @Test
    void fromEventLocation_returnsEmptyList_forNullInput() {
        assertTrue(AreaDTO.fromEventLocation(null).isEmpty());
    }

    @Test
    void fromEventLocation_returnsEmptyList_whenLocationHasNoAreas() {
        seat(1, "A1", 1, 1, null);

        assertTrue(AreaDTO.fromEventLocation(location).isEmpty());
    }

    /**
     * The point of deriving from the areas: a freshly drawn area is visible before any seat uses
     * it.
     */
    @Test
    void fromEventLocation_reportsArea_withoutAnyAssignedSeats() {
        EventLocationArea balkon = area(1, "Balkon");
        balkon.setBoundary(
                List.of(new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2)));
        seat(1, "A1", 1, 1, null);

        List<AreaDTO> areas = AreaDTO.fromEventLocation(location);

        assertEquals(1, areas.size());
        assertEquals("Balkon", areas.getFirst().name());
        assertEquals(id(1), areas.getFirst().id());
        assertTrue(areas.getFirst().seatIds().isEmpty());
        assertEquals(3, areas.getFirst().boundary().size());
    }

    @Test
    void fromEventLocation_ignoresSeatsWithoutArea() {
        area(1, "Balkon");
        seat(1, "A1", 1, 1, null);
        seat(2, "A2", 2, 1, null);
        seat(3, "A3", 3, 1, null);
        seat(4, "A4", 4, 1, location.getAreas().getFirst());

        List<AreaDTO> areas = AreaDTO.fromEventLocation(location);

        assertEquals(1, areas.size());
        assertEquals("Balkon", areas.getFirst().name());
        assertEquals(List.of(id(4)), areas.getFirst().seatIds());
    }

    @Test
    void fromEventLocation_groupsSeatsByArea_inLocationAreaOrder() {
        EventLocationArea parkett = area(1, "Parkett");
        EventLocationArea balkon = area(2, "Balkon");
        seat(1, "B1", 1, 2, balkon);
        seat(2, "A1", 1, 1, parkett);
        seat(3, "B2", 2, 2, balkon);
        seat(4, "A2", 2, 1, parkett);

        List<AreaDTO> areas = AreaDTO.fromEventLocation(location);

        assertEquals(2, areas.size());
        // Order follows the location's areas, not the order the seats happen to be in.
        assertEquals("Parkett", areas.get(0).name());
        assertEquals(List.of(id(2), id(4)), areas.get(0).seatIds());
        assertEquals("Balkon", areas.get(1).name());
        assertEquals(List.of(id(1), id(3)), areas.get(1).seatIds());
    }

    @Test
    void fromEventLocation_leavesBoundaryNull_whenAreaHasNoBoundary() {
        EventLocationArea parkett = area(1, "Parkett");
        seat(1, "A1", 1, 1, parkett);

        List<AreaDTO> areas = AreaDTO.fromEventLocation(location);

        assertEquals(1, areas.size());
        assertNull(areas.getFirst().boundary());
    }

    @Test
    void fromEventLocation_attachesBoundary_whenAreaHasOne() {
        EventLocationArea parkett = area(1, "Parkett");
        parkett.setBoundary(
                List.of(
                        new Coordinate(1, 1),
                        new Coordinate(2, 1),
                        new Coordinate(2, 2),
                        new Coordinate(1, 2)));
        EventLocationArea balkon = area(2, "Balkon");
        seat(1, "A1", 1, 1, parkett);
        seat(2, "A2", 2, 1, parkett);
        seat(3, "B1", 1, 2, balkon);

        List<AreaDTO> areas = AreaDTO.fromEventLocation(location);

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

    @Test
    void fromAreas_ignoresSeatsReferencingAnAreaOutsideTheGivenAreas() {
        EventLocationArea parkett = area(1, "Parkett");
        EventLocationArea foreign = new EventLocationArea("Foreign");
        foreign.id = id(99);
        Seat assigned = seat(1, "A1", 1, 1, parkett);
        Seat outsider = new Seat("X1", location, "Row", 9, 9, null, foreign);
        outsider.id = id(2);

        List<AreaDTO> areas = AreaDTO.fromAreas(List.of(parkett), List.of(assigned, outsider));

        assertEquals(1, areas.size());
        assertEquals(List.of(id(1)), areas.getFirst().seatIds());
    }
}
