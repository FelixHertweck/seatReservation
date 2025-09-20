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
package de.felixhertweck.seatreservation.management.dto;

import static org.junit.jupiter.api.Assertions.*;

import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import org.junit.jupiter.api.Test;

class EventLocationMakerDTOTest {

    @Test
    void testConstructorWithMarkerEntity() {
        EventLocationMarker marker = new EventLocationMarker("VIP Section", 100, 200);

        EventLocationMakerDTO dto = new EventLocationMakerDTO(marker);

        assertEquals("VIP Section", dto.label());
        assertEquals(100, dto.xCoordinate());
        assertEquals(200, dto.yCoordinate());
    }

    @Test
    void testDirectConstructor() {
        EventLocationMakerDTO dto = new EventLocationMakerDTO("Bar Area", 50, 150);

        assertEquals("Bar Area", dto.label());
        assertEquals(50, dto.xCoordinate());
        assertEquals(150, dto.yCoordinate());
    }

    @Test
    void testWithZeroCoordinates() {
        EventLocationMarker marker = new EventLocationMarker("Origin", 0, 0);
        EventLocationMakerDTO dto = new EventLocationMakerDTO(marker);

        assertEquals("Origin", dto.label());
        assertEquals(0, dto.xCoordinate());
        assertEquals(0, dto.yCoordinate());
    }

    @Test
    void testWithNegativeCoordinates() {
        EventLocationMarker marker = new EventLocationMarker("Underground", -25, -75);
        EventLocationMakerDTO dto = new EventLocationMakerDTO(marker);

        assertEquals("Underground", dto.label());
        assertEquals(-25, dto.xCoordinate());
        assertEquals(-75, dto.yCoordinate());
    }

    @Test
    void testWithBoundaryValues() {
        EventLocationMarker marker =
                new EventLocationMarker("Boundary", Integer.MAX_VALUE, Integer.MIN_VALUE);
        EventLocationMakerDTO dto = new EventLocationMakerDTO(marker);

        assertEquals("Boundary", dto.label());
        assertEquals(Integer.MAX_VALUE, dto.xCoordinate());
        assertEquals(Integer.MIN_VALUE, dto.yCoordinate());
    }

    @Test
    void testWithNullLabel() {
        EventLocationMarker marker = new EventLocationMarker(null, 10, 20);
        EventLocationMakerDTO dto = new EventLocationMakerDTO(marker);

        assertNull(dto.label());
        assertEquals(10, dto.xCoordinate());
        assertEquals(20, dto.yCoordinate());
    }

    @Test
    void testWithEmptyLabel() {
        EventLocationMarker marker = new EventLocationMarker("", 30, 40);
        EventLocationMakerDTO dto = new EventLocationMakerDTO(marker);

        assertEquals("", dto.label());
        assertEquals(30, dto.xCoordinate());
        assertEquals(40, dto.yCoordinate());
    }

    @Test
    void testWithLongLabel() {
        String longLabel = "A".repeat(1000);
        EventLocationMarker marker = new EventLocationMarker(longLabel, 1, 2);
        EventLocationMakerDTO dto = new EventLocationMakerDTO(marker);

        assertEquals(longLabel, dto.label());
        assertEquals(1, dto.xCoordinate());
        assertEquals(2, dto.yCoordinate());
    }

    @Test
    void testRecordEquality() {
        EventLocationMakerDTO dto1 = new EventLocationMakerDTO("Test", 10, 20);
        EventLocationMakerDTO dto2 = new EventLocationMakerDTO("Test", 10, 20);
        EventLocationMakerDTO dto3 = new EventLocationMakerDTO("Test", 10, 21);

        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    @Test
    void testRecordHashCode() {
        EventLocationMakerDTO dto1 = new EventLocationMakerDTO("Test", 10, 20);
        EventLocationMakerDTO dto2 = new EventLocationMakerDTO("Test", 10, 20);

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testRecordToString() {
        EventLocationMakerDTO dto = new EventLocationMakerDTO("Rest Area", 75, 125);
        String toString = dto.toString();

        assertTrue(toString.contains("EventLocationMakerDTO"));
        assertTrue(toString.contains("Rest Area"));
        assertTrue(toString.contains("75"));
        assertTrue(toString.contains("125"));
    }

    @Test
    void testConversionConsistency() {
        // Test that converting from Entity to DTO and comparing fields works correctly
        EventLocationMarker original = new EventLocationMarker("Consistent", 999, 888);
        EventLocationMakerDTO dto = new EventLocationMakerDTO(original);

        assertEquals(original.getLabel(), dto.label());
        assertEquals(original.getxCoordinate().intValue(), dto.xCoordinate());
        assertEquals(original.getyCoordinate().intValue(), dto.yCoordinate());
    }

    @Test
    void testNullCoordinatesInEntity() {
        EventLocationMarker marker = new EventLocationMarker("NullCoords", null, null);

        // This should throw a NullPointerException because the record expects int primitives
        assertThrows(
                NullPointerException.class,
                () -> {
                    new EventLocationMakerDTO(marker);
                });
    }
}
