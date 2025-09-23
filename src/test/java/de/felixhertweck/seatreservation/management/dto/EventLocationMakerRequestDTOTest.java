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

import org.junit.jupiter.api.Test;

class MakerRequestDTOTest {

    @Test
    void testDefaultConstructor() {
        MakerRequestDTO dto = new MakerRequestDTO();

        assertNull(dto.getLabel());
        assertNull(dto.getxCoordinate());
        assertNull(dto.getyCoordinate());
    }

    @Test
    void testParameterizedConstructor() {
        String label = "Main Entrance";
        Integer xCoordinate = 150;
        Integer yCoordinate = 250;

        MakerRequestDTO dto = new MakerRequestDTO(label, xCoordinate, yCoordinate);

        assertEquals(label, dto.getLabel());
        assertEquals(xCoordinate, dto.getxCoordinate());
        assertEquals(yCoordinate, dto.getyCoordinate());
    }

    @Test
    void testSettersAndGetters() {
        MakerRequestDTO dto = new MakerRequestDTO();
        String label = "Stage Area";
        Integer xCoordinate = 300;
        Integer yCoordinate = 400;

        dto.setLabel(label);
        dto.setxCoordinate(xCoordinate);
        dto.setyCoordinate(yCoordinate);

        assertEquals(label, dto.getLabel());
        assertEquals(xCoordinate, dto.getxCoordinate());
        assertEquals(yCoordinate, dto.getyCoordinate());
    }

    @Test
    void testSettersWithNullValues() {
        MakerRequestDTO dto = new MakerRequestDTO("Test", 10, 20);

        dto.setLabel(null);
        dto.setxCoordinate(null);
        dto.setyCoordinate(null);

        assertNull(dto.getLabel());
        assertNull(dto.getxCoordinate());
        assertNull(dto.getyCoordinate());
    }

    @Test
    void testWithBoundaryValues() {
        MakerRequestDTO dto = new MakerRequestDTO();

        dto.setLabel("");
        dto.setxCoordinate(Integer.MAX_VALUE);
        dto.setyCoordinate(Integer.MIN_VALUE);

        assertEquals("", dto.getLabel());
        assertEquals(Integer.MAX_VALUE, dto.getxCoordinate());
        assertEquals(Integer.MIN_VALUE, dto.getyCoordinate());
    }

    @Test
    void testWithZeroCoordinates() {
        MakerRequestDTO dto = new MakerRequestDTO("Origin", 0, 0);

        assertEquals("Origin", dto.getLabel());
        assertEquals(Integer.valueOf(0), dto.getxCoordinate());
        assertEquals(Integer.valueOf(0), dto.getyCoordinate());
    }

    @Test
    void testWithNegativeCoordinates() {
        MakerRequestDTO dto = new MakerRequestDTO("Basement", -50, -100);

        assertEquals("Basement", dto.getLabel());
        assertEquals(Integer.valueOf(-50), dto.getxCoordinate());
        assertEquals(Integer.valueOf(-100), dto.getyCoordinate());
    }

    @Test
    void testLongLabel() {
        String longLabel = "A".repeat(1000);
        MakerRequestDTO dto = new MakerRequestDTO(longLabel, 1, 2);

        assertEquals(longLabel, dto.getLabel());
    }

    @Test
    void testSetterChaining() {
        MakerRequestDTO dto = new MakerRequestDTO();

        // Test that setters work independently
        dto.setLabel("Test1");
        assertEquals("Test1", dto.getLabel());

        dto.setxCoordinate(100);
        assertEquals("Test1", dto.getLabel());
        assertEquals(Integer.valueOf(100), dto.getxCoordinate());

        dto.setyCoordinate(200);
        assertEquals("Test1", dto.getLabel());
        assertEquals(Integer.valueOf(100), dto.getxCoordinate());
        assertEquals(Integer.valueOf(200), dto.getyCoordinate());
    }

    @Test
    void testOverwriteValues() {
        MakerRequestDTO dto = new MakerRequestDTO("Initial", 1, 2);

        dto.setLabel("Updated");
        dto.setxCoordinate(10);
        dto.setyCoordinate(20);

        assertEquals("Updated", dto.getLabel());
        assertEquals(Integer.valueOf(10), dto.getxCoordinate());
        assertEquals(Integer.valueOf(20), dto.getyCoordinate());
    }
}
