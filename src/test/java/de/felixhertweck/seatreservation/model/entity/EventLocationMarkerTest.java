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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EventLocationMarkerTest {

    @Test
    void testDefaultConstructor() {
        EventLocationMarker marker = new EventLocationMarker();

        assertNull(marker.getLabel());
        assertNull(marker.getxCoordinate());
        assertNull(marker.getyCoordinate());
    }

    @Test
    void testParameterizedConstructor() {
        String label = "Entrance";
        Integer xCoordinate = 100;
        Integer yCoordinate = 200;

        EventLocationMarker marker = new EventLocationMarker(label, xCoordinate, yCoordinate);

        assertEquals(label, marker.getLabel());
        assertEquals(xCoordinate, marker.getxCoordinate());
        assertEquals(yCoordinate, marker.getyCoordinate());
    }

    @Test
    void testSettersAndGetters() {
        EventLocationMarker marker = new EventLocationMarker();
        String label = "Emergency Exit";
        Integer xCoordinate = 50;
        Integer yCoordinate = 150;

        marker.setLabel(label);
        marker.setxCoordinate(xCoordinate);
        marker.setyCoordinate(yCoordinate);

        assertEquals(label, marker.getLabel());
        assertEquals(xCoordinate, marker.getxCoordinate());
        assertEquals(yCoordinate, marker.getyCoordinate());
    }

    @Test
    void testEquals_SameObject() {
        EventLocationMarker marker = new EventLocationMarker("Stage", 300, 400);

        assertTrue(marker.equals(marker));
    }

    @Test
    void testEquals_EqualObjects() {
        EventLocationMarker marker1 = new EventLocationMarker("Bar", 10, 20);
        EventLocationMarker marker2 = new EventLocationMarker("Bar", 10, 20);

        assertTrue(marker1.equals(marker2));
        assertTrue(marker2.equals(marker1));
    }

    @Test
    void testEquals_DifferentLabel() {
        EventLocationMarker marker1 = new EventLocationMarker("Bar", 10, 20);
        EventLocationMarker marker2 = new EventLocationMarker("Restaurant", 10, 20);

        assertFalse(marker1.equals(marker2));
    }

    @Test
    void testEquals_DifferentXCoordinate() {
        EventLocationMarker marker1 = new EventLocationMarker("Bar", 10, 20);
        EventLocationMarker marker2 = new EventLocationMarker("Bar", 15, 20);

        assertFalse(marker1.equals(marker2));
    }

    @Test
    void testEquals_DifferentYCoordinate() {
        EventLocationMarker marker1 = new EventLocationMarker("Bar", 10, 20);
        EventLocationMarker marker2 = new EventLocationMarker("Bar", 10, 25);

        assertFalse(marker1.equals(marker2));
    }

    @Test
    void testEquals_NullObject() {
        EventLocationMarker marker = new EventLocationMarker("Test", 1, 2);

        assertFalse(marker.equals(null));
    }

    @Test
    void testEquals_DifferentClass() {
        EventLocationMarker marker = new EventLocationMarker("Test", 1, 2);
        String notAMarker = "Not a marker";

        assertFalse(marker.equals(notAMarker));
    }

    @Test
    void testEquals_NullValues() {
        EventLocationMarker marker1 = new EventLocationMarker(null, null, null);
        EventLocationMarker marker2 = new EventLocationMarker(null, null, null);

        assertTrue(marker1.equals(marker2));
    }

    @Test
    void testEquals_MixedNullValues() {
        EventLocationMarker marker1 = new EventLocationMarker("Test", null, 20);
        EventLocationMarker marker2 = new EventLocationMarker("Test", null, 20);
        EventLocationMarker marker3 = new EventLocationMarker("Test", 10, 20);

        assertTrue(marker1.equals(marker2));
        assertFalse(marker1.equals(marker3));
    }

    @Test
    void testHashCode_EqualObjects() {
        EventLocationMarker marker1 = new EventLocationMarker("VIP", 100, 200);
        EventLocationMarker marker2 = new EventLocationMarker("VIP", 100, 200);

        assertEquals(marker1.hashCode(), marker2.hashCode());
    }

    @Test
    void testHashCode_DifferentObjects() {
        EventLocationMarker marker1 = new EventLocationMarker("VIP", 100, 200);
        EventLocationMarker marker2 = new EventLocationMarker("Regular", 100, 200);

        assertNotEquals(marker1.hashCode(), marker2.hashCode());
    }

    @Test
    void testHashCode_NullValues() {
        EventLocationMarker marker1 = new EventLocationMarker(null, null, null);
        EventLocationMarker marker2 = new EventLocationMarker(null, null, null);

        assertEquals(marker1.hashCode(), marker2.hashCode());
    }

    @Test
    void testToString() {
        EventLocationMarker marker = new EventLocationMarker("Toilet", 75, 125);

        String toString = marker.toString();

        assertTrue(toString.contains("EventLocationMarker"));
        assertTrue(toString.contains("label='Toilet'"));
        assertTrue(toString.contains("xCoordinate=75"));
        assertTrue(toString.contains("yCoordinate=125"));
        assertTrue(toString.contains("id="));
    }

    @Test
    void testToString_NullValues() {
        EventLocationMarker marker = new EventLocationMarker(null, null, null);

        String toString = marker.toString();

        assertTrue(toString.contains("EventLocationMarker"));
        assertTrue(toString.contains("label='null'"));
        assertTrue(toString.contains("xCoordinate=null"));
        assertTrue(toString.contains("yCoordinate=null"));
    }

    @Test
    void testCoordinatesBoundaries() {
        // Test with boundary values
        EventLocationMarker marker =
                new EventLocationMarker("Boundary", Integer.MAX_VALUE, Integer.MIN_VALUE);

        assertEquals("Boundary", marker.getLabel());
        assertEquals(Integer.MAX_VALUE, marker.getxCoordinate());
        assertEquals(Integer.MIN_VALUE, marker.getyCoordinate());
    }

    @Test
    void testEmptyLabel() {
        EventLocationMarker marker = new EventLocationMarker("", 10, 20);

        assertEquals("", marker.getLabel());
        assertEquals(Integer.valueOf(10), marker.getxCoordinate());
        assertEquals(Integer.valueOf(20), marker.getyCoordinate());
    }
}
