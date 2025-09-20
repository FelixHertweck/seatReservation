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

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventTest {

    private Event event;
    private User manager;
    private EventLocation location;

    @BeforeEach
    void setUp() {
        event = new Event();

        manager = new User();
        manager.setUsername("manager");
        manager.setRoles(Set.of(Roles.MANAGER));

        location = new EventLocation();
        location.setName("Test Location");
    }

    @Test
    void testEventDefaultConstructor() {
        assertNotNull(event);
        assertNull(event.getName());
        assertNull(event.getDescription());
        assertNull(event.getStartTime());
        assertNull(event.getEndTime());
        assertNull(event.getBookingDeadline());
        assertNull(event.getEventLocation());
        assertNull(event.getManager());
        assertNotNull(event.getUserAllowances());
        assertTrue(event.getUserAllowances().isEmpty());
        assertNotNull(event.getReservations());
        assertTrue(event.getReservations().isEmpty());
    }

    @Test
    void testEventParameterizedConstructor() {
        String name = "Test Event";
        String description = "Test Description";
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusHours(2);
        LocalDateTime bookingDeadline = startTime.minusHours(1);

        Event constructedEvent =
                new Event(
                        name, description, startTime, endTime, bookingDeadline, location, manager);

        assertEquals(name, constructedEvent.getName());
        assertEquals(description, constructedEvent.getDescription());
        assertEquals(startTime, constructedEvent.getStartTime());
        assertEquals(endTime, constructedEvent.getEndTime());
        assertEquals(bookingDeadline, constructedEvent.getBookingDeadline());
        assertEquals(location, constructedEvent.getEventLocation());
        assertEquals(manager, constructedEvent.getManager());
    }

    @Test
    void testNameSetterGetter() {
        String name = "Conference 2025";
        event.setName(name);
        assertEquals(name, event.getName());
    }

    @Test
    void testDescriptionSetterGetter() {
        String description = "Annual tech conference with industry leaders";
        event.setDescription(description);
        assertEquals(description, event.getDescription());
    }

    @Test
    void testStartTimeSetterGetter() {
        LocalDateTime startTime = LocalDateTime.of(2025, 12, 25, 10, 0);
        event.setStartTime(startTime);
        assertEquals(startTime, event.getStartTime());
    }

    @Test
    void testEndTimeSetterGetter() {
        LocalDateTime endTime = LocalDateTime.of(2025, 12, 25, 18, 0);
        event.setEndTime(endTime);
        assertEquals(endTime, event.getEndTime());
    }

    @Test
    void testBookingDeadlineSetterGetter() {
        LocalDateTime deadline = LocalDateTime.of(2025, 12, 24, 23, 59);
        event.setBookingDeadline(deadline);
        assertEquals(deadline, event.getBookingDeadline());
    }

    @Test
    void testEventLocationSetterGetter() {
        event.setEventLocation(location);
        assertEquals(location, event.getEventLocation());
    }

    @Test
    void testManagerSetterGetter() {
        event.setManager(manager);
        assertEquals(manager, event.getManager());
    }

    @Test
    void testEventTimingValidation() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusDays(1);
        LocalDateTime endTime = startTime.plusHours(3);
        LocalDateTime bookingDeadline = startTime.minusHours(2);

        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setBookingDeadline(bookingDeadline);

        // Verify booking deadline is before start time
        assertTrue(event.getBookingDeadline().isBefore(event.getStartTime()));
        // Verify end time is after start time
        assertTrue(event.getEndTime().isAfter(event.getStartTime()));
    }

    @Test
    void testEventWithPastDates() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        event.setStartTime(pastDate);
        event.setEndTime(pastDate.plusHours(2));
        event.setBookingDeadline(pastDate.minusHours(1));

        assertEquals(pastDate, event.getStartTime());
        assertTrue(event.getStartTime().isBefore(LocalDateTime.now()));
    }

    @Test
    void testEventWithFutureDates() {
        LocalDateTime futureDate = LocalDateTime.now().plusMonths(1);
        event.setStartTime(futureDate);
        event.setEndTime(futureDate.plusHours(4));
        event.setBookingDeadline(futureDate.minusDays(1));

        assertEquals(futureDate, event.getStartTime());
        assertTrue(event.getStartTime().isAfter(LocalDateTime.now()));
    }

    @Test
    void testEmptyAndNullStrings() {
        event.setName("");
        event.setDescription(null);

        assertEquals("", event.getName());
        assertNull(event.getDescription());
    }

    @Test
    void testLongTextFields() {
        String longName = "A".repeat(255);
        String longDescription = "B".repeat(1000);

        event.setName(longName);
        event.setDescription(longDescription);

        assertEquals(longName, event.getName());
        assertEquals(longDescription, event.getDescription());
    }

    @Test
    void testSpecialCharactersInText() {
        String nameWithSpecialChars = "Event 2025: Tech & Innovation!";
        String descriptionWithSpecialChars =
                "Join us for an exciting event featuring speakers like José María & François";

        event.setName(nameWithSpecialChars);
        event.setDescription(descriptionWithSpecialChars);

        assertEquals(nameWithSpecialChars, event.getName());
        assertEquals(descriptionWithSpecialChars, event.getDescription());
    }

    @Test
    void testEqualsAndHashCode() {
        Event event1 = new Event();
        event1.setName("Test Event");
        event1.setDescription("Description");

        Event event2 = new Event();
        event2.setName("Test Event");
        event2.setDescription("Description");

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());

        // Test inequality
        event2.setName("Different Event");
        assertNotEquals(event1, event2);
    }

    @Test
    void testToString() {
        event.setName("Test Event");
        event.setDescription("Test Description");

        String toString = event.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Test Event"));
    }

    @Test
    void testUserAllowancesCollection() {
        assertNotNull(event.getUserAllowances());
        assertTrue(event.getUserAllowances().isEmpty());

        // UserAllowances should be modifiable
        EventUserAllowance allowance = new EventUserAllowance();
        event.getUserAllowances().add(allowance);
        assertEquals(1, event.getUserAllowances().size());
    }

    @Test
    void testReservationsCollection() {
        assertNotNull(event.getReservations());
        assertTrue(event.getReservations().isEmpty());

        // Reservations should be modifiable
        Reservation reservation = new Reservation();
        event.getReservations().add(reservation);
        assertEquals(1, event.getReservations().size());
    }

    @Test
    void testEventWithNullManager() {
        event.setManager(null);
        assertNull(event.getManager());
    }

    @Test
    void testEventWithNullLocation() {
        event.setEventLocation(null);
        assertNull(event.getEventLocation());
    }

    @Test
    void testSameDayEvent() {
        LocalDateTime eventDay = LocalDateTime.of(2025, 6, 15, 9, 0);
        LocalDateTime startTime = eventDay;
        LocalDateTime endTime = eventDay.withHour(17);
        LocalDateTime bookingDeadline = eventDay.minusDays(1);

        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setBookingDeadline(bookingDeadline);

        assertEquals(startTime.toLocalDate(), endTime.toLocalDate());
        assertTrue(event.getEndTime().isAfter(event.getStartTime()));
    }

    @Test
    void testMultiDayEvent() {
        LocalDateTime startTime = LocalDateTime.of(2025, 6, 15, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 6, 17, 17, 0); // 3-day event
        LocalDateTime bookingDeadline = startTime.minusWeeks(1);

        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setBookingDeadline(bookingDeadline);

        assertNotEquals(startTime.toLocalDate(), endTime.toLocalDate());
        assertTrue(event.getEndTime().isAfter(event.getStartTime()));
        assertTrue(event.getBookingDeadline().isBefore(event.getStartTime()));
    }

    @Test
    void testCompleteEventSetup() {
        // Test a fully configured event
        String eventName = "Annual Conference 2025";
        String eventDescription = "The biggest tech conference of the year";
        LocalDateTime startTime = LocalDateTime.of(2025, 9, 15, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 9, 15, 18, 0);
        LocalDateTime bookingDeadline = LocalDateTime.of(2025, 9, 1, 23, 59);

        event.setName(eventName);
        event.setDescription(eventDescription);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setBookingDeadline(bookingDeadline);
        event.setEventLocation(location);
        event.setManager(manager);

        assertEquals(eventName, event.getName());
        assertEquals(eventDescription, event.getDescription());
        assertEquals(startTime, event.getStartTime());
        assertEquals(endTime, event.getEndTime());
        assertEquals(bookingDeadline, event.getBookingDeadline());
        assertEquals(location, event.getEventLocation());
        assertEquals(manager, event.getManager());
        assertNotNull(event.getUserAllowances());
        assertNotNull(event.getReservations());
    }
}
