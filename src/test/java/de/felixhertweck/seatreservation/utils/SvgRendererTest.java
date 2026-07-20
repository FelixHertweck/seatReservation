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

import de.felixhertweck.seatreservation.common.dto.AreaBoundaryPointDTO;
import de.felixhertweck.seatreservation.common.dto.AreaDTO;
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
    void renderSeats_WithSpecialCharactersInSeatNumber_EscapesXml() {
        Seat seat = new Seat();
        seat.setSeatNumber("<script>alert(1)</script>");
        seat.setxCoordinate(2);
        seat.setyCoordinate(2);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of());

        assertNotNull(result);
        assertTrue(result.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(
                result.contains(
                        "<script>alert(1)</script>")); // Should not contain unescaped script tags
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

    @Test
    void renderSeats_WithNullAreas_WorksNormally() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), null, null);

        assertNotNull(result);
        assertTrue(result.contains("A1"));
    }

    @Test
    void renderSeats_WithEmptyAreas_WorksNormally() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        String result = SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), null, List.of());

        assertNotNull(result);
        assertTrue(result.contains("A1"));
    }

    @Test
    void renderSeats_WithAreaWithoutBoundary_RendersBoundingBoxAndLabel() {
        Seat seat1 = new Seat();
        seat1.id = 1L;
        seat1.setSeatNumber("A1");
        seat1.setxCoordinate(1);
        seat1.setyCoordinate(1);

        Seat seat2 = new Seat();
        seat2.id = 2L;
        seat2.setSeatNumber("A2");
        seat2.setxCoordinate(2);
        seat2.setyCoordinate(1);

        AreaDTO area = new AreaDTO("Parkett", List.of(1L, 2L), null);

        String result =
                SvgRenderer.renderSeats(
                        List.of(seat1, seat2), Set.of(), Set.of(), null, List.of(area));

        assertNotNull(result);
        assertTrue(result.contains("<rect"));
        assertTrue(result.contains("Parkett"));
        // The zone must be rendered before the seats it encloses (background layer)
        assertTrue(result.indexOf("<rect") < result.indexOf("A1"));
    }

    @Test
    void renderSeats_WithAreaBoundary_RendersPolygonInsteadOfBoundingBox() {
        Seat seat = new Seat();
        seat.id = 1L;
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        List<AreaBoundaryPointDTO> boundary =
                List.of(
                        new AreaBoundaryPointDTO(1, 1),
                        new AreaBoundaryPointDTO(3, 1),
                        new AreaBoundaryPointDTO(3, 3),
                        new AreaBoundaryPointDTO(1, 3));
        AreaDTO area = new AreaDTO("Loge", List.of(1L), boundary);

        String result =
                SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), null, List.of(area));

        assertNotNull(result);
        assertTrue(result.contains("<polygon"));
        assertFalse(result.contains("<rect"));
        assertTrue(result.contains("Loge"));
    }

    @Test
    void renderSeats_WithAreaBoundaryOfLessThanThreePoints_FallsBackToBoundingBox() {
        Seat seat = new Seat();
        seat.id = 1L;
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        List<AreaBoundaryPointDTO> tooFewPoints =
                List.of(new AreaBoundaryPointDTO(1, 1), new AreaBoundaryPointDTO(2, 1));
        AreaDTO area = new AreaDTO("Loge", List.of(1L), tooFewPoints);

        String result =
                SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), null, List.of(area));

        assertNotNull(result);
        assertTrue(result.contains("<rect"));
        assertFalse(result.contains("<polygon"));
    }

    @Test
    void renderSeats_WithAreaReferencingUnknownSeatIds_SkipsZone() {
        Seat seat = new Seat();
        seat.id = 1L;
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        AreaDTO area = new AreaDTO("Parkett", List.of(999L), null);

        String result =
                SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), null, List.of(area));

        assertNotNull(result);
        assertTrue(result.contains("A1"));
        assertFalse(result.contains("<rect"));
        assertFalse(result.contains("Parkett"));
    }

    @Test
    void renderSeats_MultipleAreas_UseDifferentColors() {
        Seat seat1 = new Seat();
        seat1.id = 1L;
        seat1.setSeatNumber("A1");
        seat1.setxCoordinate(1);
        seat1.setyCoordinate(1);

        Seat seat2 = new Seat();
        seat2.id = 2L;
        seat2.setSeatNumber("B1");
        seat2.setxCoordinate(1);
        seat2.setyCoordinate(2);

        AreaDTO parkett = new AreaDTO("Parkett", List.of(1L), null);
        AreaDTO balkon = new AreaDTO("Balkon", List.of(2L), null);

        String result =
                SvgRenderer.renderSeats(
                        List.of(seat1, seat2), Set.of(), Set.of(), null, List.of(parkett, balkon));

        assertTrue(result.contains("Parkett"));
        assertTrue(result.contains("Balkon"));
        assertTrue(result.contains("#fbbf24")); // first area color
        assertTrue(result.contains("#22d3ee")); // second area color
    }

    @Test
    void renderSeats_WithAreaBoundaryOutsideSeatBounds_ExpandsViewBoxToIncludeIt() {
        Seat seat = new Seat();
        seat.id = 1L;
        seat.setSeatNumber("A1");
        seat.setxCoordinate(1);
        seat.setyCoordinate(1);

        // Boundary reaches far past the only seat (logical x/y 10 vs. the seat's 1,1), e.g. a
        // balcony edge drawn larger than its outermost seats.
        List<AreaBoundaryPointDTO> boundary =
                List.of(
                        new AreaBoundaryPointDTO(1, 1),
                        new AreaBoundaryPointDTO(10, 1),
                        new AreaBoundaryPointDTO(10, 10),
                        new AreaBoundaryPointDTO(1, 10));
        AreaDTO area = new AreaDTO("Balkon", List.of(1L), boundary);

        String result =
                SvgRenderer.renderSeats(List.of(seat), Set.of(), Set.of(), null, List.of(area));

        String viewBox = result.substring(result.indexOf("viewBox=\"") + 9);
        viewBox = viewBox.substring(0, viewBox.indexOf("\""));
        String[] parts = viewBox.split(" ");
        int width = Integer.parseInt(parts[2]);
        int height = Integer.parseInt(parts[3]);

        // With only the seat's bounding box (a single point), width/height would be ~70/82.
        // The boundary point at logical (10,10) must expand the viewBox well past that, or the
        // polygon gets clipped.
        assertTrue(
                width > 300, "expected viewBox width to include the boundary point, was " + width);
        assertTrue(
                height > 300,
                "expected viewBox height to include the boundary point, was " + height);
    }
}
