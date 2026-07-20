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
        assertNull(marker.getCoordinate());
    }

    @Test
    void testParameterizedConstructor() {
        EventLocationMarker marker = new EventLocationMarker("Entrance", 100, 200);

        assertEquals("Entrance", marker.getLabel());
        assertEquals(new Coordinate(100, 200), marker.getCoordinate());
    }

    @Test
    void testSettersAndGetters() {
        EventLocationMarker marker = new EventLocationMarker();

        marker.setLabel("Emergency Exit");
        marker.setCoordinate(new Coordinate(50, 150));

        assertEquals("Emergency Exit", marker.getLabel());
        assertEquals(new Coordinate(50, 150), marker.getCoordinate());
    }

    @Test
    void testEquals_SameObject() {
        EventLocationMarker marker = new EventLocationMarker("Stage", 300, 400);

        assertEquals(marker, marker);
    }

    @Test
    void testEquals_EqualObjects() {
        EventLocationMarker marker1 = new EventLocationMarker("Bar", 10, 20);
        EventLocationMarker marker2 = new EventLocationMarker("Bar", 10, 20);

        assertEquals(marker1, marker2);
        assertEquals(marker2, marker1);
    }

    @Test
    void testEquals_DifferentLabel() {
        EventLocationMarker marker1 = new EventLocationMarker("Bar", 10, 20);
        EventLocationMarker marker2 = new EventLocationMarker("Restaurant", 10, 20);

        assertNotEquals(marker1, marker2);
    }

    @Test
    void testEquals_DifferentCoordinate() {
        EventLocationMarker marker1 = new EventLocationMarker("Bar", 10, 20);
        EventLocationMarker marker2 = new EventLocationMarker("Bar", 15, 20);

        assertNotEquals(marker1, marker2);
    }

    @Test
    void testEquals_NullObject() {
        EventLocationMarker marker = new EventLocationMarker("Test", 1, 2);

        assertNotEquals(null, marker);
    }

    @Test
    void testEquals_DifferentClass() {
        EventLocationMarker marker = new EventLocationMarker("Test", 1, 2);

        assertNotEquals("Not a marker", marker);
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
    void testToString() {
        EventLocationMarker marker = new EventLocationMarker("Toilet", 75, 125);

        String toString = marker.toString();

        assertTrue(toString.contains("EventLocationMarker"));
        assertTrue(toString.contains("label='Toilet'"));
        assertTrue(toString.contains("coordinate=" + new Coordinate(75, 125)));
        assertTrue(toString.contains("id="));
    }

    @Test
    void testCoordinatesBoundaries() {
        EventLocationMarker marker =
                new EventLocationMarker("Boundary", Integer.MAX_VALUE, Integer.MIN_VALUE);

        assertEquals("Boundary", marker.getLabel());
        assertEquals(new Coordinate(Integer.MAX_VALUE, Integer.MIN_VALUE), marker.getCoordinate());
    }

    @Test
    void testEmptyLabel() {
        EventLocationMarker marker = new EventLocationMarker("", 10, 20);

        assertEquals("", marker.getLabel());
        assertEquals(new Coordinate(10, 20), marker.getCoordinate());
    }
}
