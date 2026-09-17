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
package de.felixhertweck.seatreservation.model.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the GROUP BY aggregation queries behind the manager dashboard overview - that counts are
 * summed correctly across multiple reservations, and that the manager-scoped variants only count
 * reservations for events the given manager actually manages. Added as a PR #590 Copilot review
 * follow-up: these queries were previously only exercised via mocks in {@code OverviewServiceTest},
 * so a broken GROUP BY or a manager-scoping leak would not have been caught.
 */
@QuarkusTest
class ReservationRepositoryTest {

    @Inject ReservationRepository reservationRepository;
    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject SeatRepository seatRepository;

    private User managerA;
    private User managerB;
    private User regularUser;
    private Event eventA;
    private Event eventB;
    private EventLocation location;
    private List<UUID> reservationIds;
    private List<Seat> seats;

    @BeforeEach
    @Transactional
    void setUp() {
        managerA = userRepository.findByUsernameOptional("manager").orElseThrow();
        managerB = userRepository.findByUsernameOptional("supervisor").orElseThrow();
        regularUser = userRepository.findByUsernameOptional("user").orElseThrow();

        location = new EventLocation();
        location.setName("Reservation Repository Test Location");
        eventLocationRepository.persist(location);

        eventA = newTestEvent("Reservation Repository Test Event A", Set.of(managerA));
        eventRepository.persist(eventA);

        eventB = newTestEvent("Reservation Repository Test Event B", Set.of(managerB));
        eventRepository.persist(eventB);

        // Each reservation needs its own seat: the (event_id, seat_id) unique constraint would
        // otherwise reject the two RESERVED rows for regularUser/eventA below.
        seats = List.of(newSeat("A1"), newSeat("A2"), newSeat("A3"), newSeat("B1"));
        seatRepository.persist(seats);

        List<Reservation> reservations =
                List.of(
                        newReservation(
                                regularUser, eventA, seats.get(0), ReservationStatus.RESERVED),
                        newReservation(
                                regularUser, eventA, seats.get(1), ReservationStatus.RESERVED),
                        newReservation(managerA, eventA, seats.get(2), ReservationStatus.BLOCKED),
                        newReservation(
                                regularUser, eventB, seats.get(3), ReservationStatus.RESERVED));
        reservationRepository.persistAll(reservations);
        reservationIds = reservations.stream().map(r -> r.id).toList();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        reservationRepository.deleteByIds(reservationIds);
        seats.forEach(seatRepository::delete);
        eventRepository.delete(eventA);
        eventRepository.delete(eventB);
        eventLocationRepository.delete(location);
    }

    private Seat newSeat(String seatNumber) {
        return new Seat(seatNumber, "A", location);
    }

    @Test
    void getReservationCounts_SumsAcrossAllManagers() {
        Map<ReservationStatus, Long> counts = reservationRepository.getReservationCounts();

        assertEquals(3L, counts.get(ReservationStatus.RESERVED));
        assertEquals(1L, counts.get(ReservationStatus.BLOCKED));
    }

    @Test
    void getReservationCountsByManager_OnlyCountsOwnEvents() {
        Map<ReservationStatus, Long> countsA =
                reservationRepository.getReservationCountsByManager(managerA);
        Map<ReservationStatus, Long> countsB =
                reservationRepository.getReservationCountsByManager(managerB);

        assertEquals(2L, countsA.get(ReservationStatus.RESERVED));
        assertEquals(1L, countsA.get(ReservationStatus.BLOCKED));

        assertEquals(1L, countsB.get(ReservationStatus.RESERVED));
        assertFalse(countsB.containsKey(ReservationStatus.BLOCKED));
    }

    @Test
    void getReservedSeatCountsByEventAndUser_SumsAcrossAllManagers() {
        Map<String, Long> counts = reservationRepository.getReservedSeatCountsByEventAndUser();

        assertEquals(2L, counts.get(eventA.id + ":" + regularUser.id));
        assertEquals(1L, counts.get(eventB.id + ":" + regularUser.id));
    }

    @Test
    void getReservedSeatCountsByEventAndUserByManager_OnlyCountsOwnEvents() {
        Map<String, Long> countsA =
                reservationRepository.getReservedSeatCountsByEventAndUserByManager(managerA);
        Map<String, Long> countsB =
                reservationRepository.getReservedSeatCountsByEventAndUserByManager(managerB);

        assertEquals(2L, countsA.get(eventA.id + ":" + regularUser.id));
        assertFalse(countsA.containsKey(eventB.id + ":" + regularUser.id));

        assertEquals(1L, countsB.get(eventB.id + ":" + regularUser.id));
        assertFalse(countsB.containsKey(eventA.id + ":" + regularUser.id));
    }

    private static Event newTestEvent(String name, Set<User> managers) {
        Event event = new Event();
        event.setName(name);
        event.setStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        event.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(2).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        event.setManagers(managers);
        return event;
    }

    private static Reservation newReservation(
            User user, Event event, Seat seat, ReservationStatus status) {
        return new Reservation(user, event, seat, Instant.now(), status, null);
    }
}
