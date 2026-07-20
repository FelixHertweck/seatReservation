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

class EventLocationEntranceTest {

    @Test
    void testDefaultConstructor() {
        EventLocationEntrance entrance = new EventLocationEntrance();

        assertNull(entrance.getName());
    }

    @Test
    void testParameterizedConstructor() {
        EventLocationEntrance entrance = new EventLocationEntrance("Entrance A");

        assertEquals("Entrance A", entrance.getName());
    }

    @Test
    void testSettersAndGetters() {
        EventLocationEntrance entrance = new EventLocationEntrance();

        entrance.setName("Entrance B");
        EventLocation location = new EventLocation();
        entrance.setEventLocation(location);

        assertEquals("Entrance B", entrance.getName());
        assertEquals(location, entrance.getEventLocation());
    }

    @Test
    void testEquals_SameObject() {
        EventLocationEntrance entrance = new EventLocationEntrance("Entrance A");

        assertEquals(entrance, entrance);
    }

    @Test
    void testEquals_EqualObjects() {
        EventLocationEntrance entrance1 = new EventLocationEntrance("Entrance A");
        EventLocationEntrance entrance2 = new EventLocationEntrance("Entrance A");

        assertEquals(entrance1, entrance2);
        assertEquals(entrance2, entrance1);
    }

    @Test
    void testEquals_DifferentName() {
        EventLocationEntrance entrance1 = new EventLocationEntrance("Entrance A");
        EventLocationEntrance entrance2 = new EventLocationEntrance("Entrance B");

        assertNotEquals(entrance1, entrance2);
    }

    @Test
    void testEquals_NullObject() {
        EventLocationEntrance entrance = new EventLocationEntrance("Entrance A");

        assertNotEquals(null, entrance);
    }

    @Test
    void testEquals_DifferentClass() {
        EventLocationEntrance entrance = new EventLocationEntrance("Entrance A");

        assertNotEquals("Not an entrance", entrance);
    }

    @Test
    void testHashCode_EqualObjects() {
        EventLocationEntrance entrance1 = new EventLocationEntrance("Entrance A");
        EventLocationEntrance entrance2 = new EventLocationEntrance("Entrance A");

        assertEquals(entrance1.hashCode(), entrance2.hashCode());
    }

    @Test
    void testHashCode_DifferentObjects() {
        EventLocationEntrance entrance1 = new EventLocationEntrance("Entrance A");
        EventLocationEntrance entrance2 = new EventLocationEntrance("Entrance B");

        assertNotEquals(entrance1.hashCode(), entrance2.hashCode());
    }

    @Test
    void testEquals_SameNameInDifferentLocations_NotEqual() {
        EventLocation locationA = new EventLocation();
        locationA.id = 1L;
        EventLocation locationB = new EventLocation();
        locationB.id = 2L;

        EventLocationEntrance entrance1 = new EventLocationEntrance("Entrance A");
        entrance1.setEventLocation(locationA);
        EventLocationEntrance entrance2 = new EventLocationEntrance("Entrance A");
        entrance2.setEventLocation(locationB);

        assertNotEquals(entrance1, entrance2);
    }

    @Test
    void testEquals_PersistedComparesById() {
        EventLocationEntrance entrance1 = new EventLocationEntrance("Entrance A");
        entrance1.id = 42L;
        // Differs in every other field: once persisted, only the id decides.
        EventLocationEntrance entrance2 = new EventLocationEntrance("Entrance B");
        entrance2.id = 42L;

        assertEquals(entrance1, entrance2);
        assertEquals(entrance1.hashCode(), entrance2.hashCode());
    }

    @Test
    void testEquals_DifferentIds_NotEqual() {
        EventLocationEntrance entrance1 = new EventLocationEntrance("Entrance A");
        entrance1.id = 1L;
        EventLocationEntrance entrance2 = new EventLocationEntrance("Entrance A");
        entrance2.id = 2L;

        assertNotEquals(entrance1, entrance2);
    }

    @Test
    void testToString() {
        EventLocationEntrance entrance = new EventLocationEntrance("Entrance A");

        String toString = entrance.toString();

        assertTrue(toString.contains("EventLocationEntrance"));
        assertTrue(toString.contains("name='Entrance A'"));
        assertTrue(toString.contains("id="));
    }

    @Test
    void testEmptyName() {
        EventLocationEntrance entrance = new EventLocationEntrance("");

        assertEquals("", entrance.getName());
    }
}
