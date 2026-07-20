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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EventLocationAreaTest {

    @Test
    void testDefaultConstructor() {
        EventLocationArea area = new EventLocationArea();

        assertNull(area.getName());
        assertTrue(area.getBoundary().isEmpty());
    }

    @Test
    void testParameterizedConstructor() {
        EventLocationArea area = new EventLocationArea("Parkett");

        assertEquals("Parkett", area.getName());
        assertTrue(area.getBoundary().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        EventLocationArea area = new EventLocationArea();

        area.setName("Balkon");
        area.setBoundary(List.of(new Coordinate(1, 2), new Coordinate(3, 4)));

        assertEquals("Balkon", area.getName());
        assertEquals(List.of(new Coordinate(1, 2), new Coordinate(3, 4)), area.getBoundary());
    }

    @Test
    void testEquals_SameObject() {
        EventLocationArea area = new EventLocationArea("Loge");

        assertEquals(area, area);
    }

    @Test
    void testEquals_EqualObjects() {
        EventLocationArea area1 = new EventLocationArea("Parkett");
        EventLocationArea area2 = new EventLocationArea("Parkett");

        assertEquals(area1, area2);
        assertEquals(area2, area1);
    }

    @Test
    void testEquals_DifferentName() {
        EventLocationArea area1 = new EventLocationArea("Parkett");
        EventLocationArea area2 = new EventLocationArea("Balkon");

        assertNotEquals(area1, area2);
    }

    @Test
    void testEquals_DifferentBoundary() {
        EventLocationArea area1 = new EventLocationArea("Parkett");
        area1.setBoundary(List.of(new Coordinate(1, 1)));
        EventLocationArea area2 = new EventLocationArea("Parkett");
        area2.setBoundary(List.of(new Coordinate(2, 2)));

        assertNotEquals(area1, area2);
    }

    @Test
    void testEquals_NullObject() {
        EventLocationArea area = new EventLocationArea("Test");

        assertNotEquals(null, area);
    }

    @Test
    void testEquals_DifferentClass() {
        EventLocationArea area = new EventLocationArea("Test");

        assertNotEquals("Not an area", area);
    }

    @Test
    void testHashCode_EqualObjects() {
        EventLocationArea area1 = new EventLocationArea("VIP");
        EventLocationArea area2 = new EventLocationArea("VIP");

        assertEquals(area1.hashCode(), area2.hashCode());
    }

    @Test
    void testHashCode_DifferentObjects() {
        EventLocationArea area1 = new EventLocationArea("VIP");
        EventLocationArea area2 = new EventLocationArea("Regular");

        assertNotEquals(area1.hashCode(), area2.hashCode());
    }

    @Test
    void testEquals_SameNameInDifferentLocations_NotEqual() {
        EventLocation locationA = new EventLocation();
        locationA.id = 1L;
        EventLocation locationB = new EventLocation();
        locationB.id = 2L;

        EventLocationArea area1 = new EventLocationArea("Parkett");
        area1.setEventLocation(locationA);
        EventLocationArea area2 = new EventLocationArea("Parkett");
        area2.setEventLocation(locationB);

        assertNotEquals(area1, area2);
    }

    @Test
    void testEquals_PersistedComparesById() {
        EventLocationArea area1 = new EventLocationArea("Parkett");
        area1.id = 42L;
        // Differs in every other field: once persisted, only the id decides.
        EventLocationArea area2 = new EventLocationArea("Balkon");
        area2.id = 42L;
        area2.setBoundary(List.of(new Coordinate(1, 2)));

        assertEquals(area1, area2);
        assertEquals(area1.hashCode(), area2.hashCode());
    }

    @Test
    void testEquals_DifferentIds_NotEqual() {
        EventLocationArea area1 = new EventLocationArea("Parkett");
        area1.id = 1L;
        EventLocationArea area2 = new EventLocationArea("Parkett");
        area2.id = 2L;

        assertNotEquals(area1, area2);
    }

    @Test
    void testToString() {
        EventLocationArea area = new EventLocationArea("Parkett");

        String toString = area.toString();

        assertTrue(toString.contains("EventLocationArea"));
        assertTrue(toString.contains("name='Parkett'"));
        assertTrue(toString.contains("eventLocationId="));
        assertTrue(toString.contains("id="));
        // boundary is lazy and toString() is reached from debug logging via Seat#toString(),
        // so it must not be rendered: that would cost a query, or throw outside a session.
        assertFalse(toString.contains("boundary="));
    }

    @Test
    void testEmptyName() {
        EventLocationArea area = new EventLocationArea("");

        assertEquals("", area.getName());
        assertTrue(area.getBoundary().isEmpty());
    }
}
