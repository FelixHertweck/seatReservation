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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Seat;
import org.junit.jupiter.api.Test;

class SvgRendererTest {

    @Test
    void renderSeats_EmptySeats_ReturnsEmptyString() {
        String result = SvgRenderer.renderSeats(List.of(), Set.of(), Set.of());
        assertEquals("", result);
    }

    @Test
    void renderSeats_NullSeats_ReturnsEmptyString() {
        String result = SvgRenderer.renderSeats(null, Set.of(), Set.of());
        assertEquals("", result);
    }

    @Test
    void renderSeats_SingleSeat_ReturnsValidSvg() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of());

        assertNotNull(result);
        assertTrue(result.contains("<svg"));
        assertTrue(result.contains("</svg>"));
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("#CCCCCC")); // Default color
    }

    @Test
    void renderSeats_WithNewReservation_ShowsCorrectColors() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of("A1"), Set.of());

        assertTrue(result.contains("#2B7FFF")); // New reservation color
    }

    @Test
    void renderSeats_WithExistingReservation_ShowsCorrectColors() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of("A1"));

        assertTrue(result.contains("#F0B100")); // Existing reservation color
    }

    @Test
    void renderSeats_WithMarkers_IncludesMarkerLabels() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(2);
        seat.setyCoordinate(2);

        EventLocationMarker marker = new EventLocationMarker("Entrance", 1, 1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), List.of(marker));

        assertNotNull(result);
        assertTrue(result.contains("<svg"));
        assertTrue(result.contains("</svg>"));
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("Entrance"));
        // Check that marker is rendered before seats (background layer)
        int markerIndex = result.indexOf("Entrance");
        int seatIndex = result.indexOf("A1");
        assertTrue(markerIndex < seatIndex, "Markers should be rendered before seats");
    }

    @Test
    void renderSeats_WithNullMarkers_WorksNormally() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), null);

        assertNotNull(result);
        assertTrue(result.contains("A1"));
    }

    @Test
    void renderSeats_WithEmptyMarkers_WorksNormally() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), List.of());

        assertNotNull(result);
        assertTrue(result.contains("A1"));
    }

    @Test
    void renderSeats_WithMarkerNullCoordinates_SkipsMarker() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        EventLocationMarker marker = new EventLocationMarker("Invalid", null, null);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), List.of(marker));

        assertNotNull(result);
        assertTrue(result.contains("A1"));
        assertFalse(result.contains("Invalid"));
    }

    @Test
    void renderSeats_WithMarkerEmptyLabel_SkipsMarker() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        EventLocationMarker marker = new EventLocationMarker("", 1, 1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), List.of(marker));

        assertNotNull(result);
        assertTrue(result.contains("A1"));
        // Empty label should be skipped, so no marker text should be present
        // We can verify this by checking that there's no extra text element beyond the seat
    }

    @Test
    void renderSeats_WithSpecialCharactersInMarkerLabel_EscapesXml() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(2);
        seat.setyCoordinate(2);

        EventLocationMarker marker = new EventLocationMarker("Entry & Exit", 1, 1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), List.of(marker));

        assertNotNull(result);
        assertTrue(result.contains("Entry &amp; Exit"));
        assertFalse(result.contains("Entry & Exit")); // Should not contain unescaped ampersand
    }

    @Test
    void renderSeats_CalculatesBoundingBoxWithMarkers() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(3);
        seat.setyCoordinate(3);

        // Marker extends the bounding box
        EventLocationMarker marker = new EventLocationMarker("Far", 10, 10);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), List.of(marker));

        assertNotNull(result);
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("Far"));
        // ViewBox should include the marker's extended coordinates
        assertTrue(result.contains("viewBox"));
    }

    @Test
    void renderSeats_BackwardsCompatibility_WithoutMarkersWorks() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        // Call the old method without markers parameter
        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of());

        assertNotNull(result);
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("<svg"));
        assertTrue(result.contains("</svg>"));
    }

    @Test
    void renderSeats_MultipleMarkersAndSeats_RendersAll() {
        Seat seat1 = new Seat();
        seat1.setSeatNumber("A1");
        seat1.setxCoordinate(2);
        seat1.setyCoordinate(2);

        Seat seat2 = new Seat();
        seat2.setSeatNumber("B2");
        seat2.setxCoordinate(3);
        seat2.setyCoordinate(3);

        EventLocationMarker marker1 = new EventLocationMarker("Entrance", 1, 1);
        EventLocationMarker marker2 = new EventLocationMarker("Exit", 4, 4);

        String result =
                SvgRenderer.renderSeats(
                        List.of(seat1, seat2),
                        Set.of("A1"),
                        Set.of("B2"),
                        List.of(marker1, marker2));

        assertNotNull(result);
        assertTrue(result.contains("A1"));
        assertTrue(result.contains("B2"));
        assertTrue(result.contains("Entrance"));
        assertTrue(result.contains("Exit"));
        assertTrue(result.contains("#2B7FFF")); // New reservation
        assertTrue(result.contains("#F0B100")); // Existing reservation
    }
}
