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
package de.felixhertweck.seatreservation.management.service;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.management.dto.ManagementOverviewDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class OverviewServiceTest {

    @InjectMock EventRepository eventRepository;
    @InjectMock EventLocationRepository eventLocationRepository;
    @InjectMock ReservationRepository reservationRepository;
    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;
    @InjectMock UserRepository userRepository;

    @Inject OverviewService overviewService;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;

    private EventLocation location;
    private Event upcomingEvent;
    private Event pastEvent;
    private Reservation reservation;
    private Reservation blockedReservation;
    private EventUserAllowance allowance;

    @BeforeEach
    void setUp() {
        Mockito.reset(
                eventRepository,
                eventLocationRepository,
                reservationRepository,
                eventUserAllowanceRepository,
                userRepository);

        adminUser =
                new User(
                        "admin",
                        "admin@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "A",
                        "U",
                        Set.of(Roles.ADMIN),
                        Set.of());
        adminUser.id = id(1);

        managerUser =
                new User(
                        "manager",
                        "manager@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "M",
                        "U",
                        Set.of(Roles.MANAGER),
                        Set.of());
        managerUser.id = id(2);

        regularUser =
                new User(
                        "user",
                        "user@example.com",
                        true,
                        false,
                        "h",
                        "s",
                        "R",
                        "U",
                        Set.of(Roles.USER),
                        Set.of());
        regularUser.id = id(3);

        adminAuth = new AuthenticatedUser(adminUser.id, adminUser.getRoles());
        managerAuth = new AuthenticatedUser(managerUser.id, managerUser.getRoles());

        when(userRepository.getReference(managerUser.id)).thenReturn(managerUser);
        when(userRepository.getReference(adminUser.id)).thenReturn(adminUser);

        location = new EventLocation("Hall A", "Street 1", managerUser);
        location.id = id(10);

        upcomingEvent =
                new Event(
                        "Future Event",
                        "Desc",
                        Instant.now().plus(Duration.ofDays(5)),
                        Instant.now().plus(Duration.ofDays(6)),
                        Instant.now().plus(Duration.ofDays(4)),
                        Instant.now().minus(Duration.ofDays(1)),
                        location,
                        managerUser,
                        null,
                        Set.of());
        upcomingEvent.id = id(100);

        pastEvent =
                new Event(
                        "Past Event",
                        "Desc",
                        Instant.now().minus(Duration.ofDays(10)),
                        Instant.now().minus(Duration.ofDays(9)),
                        Instant.now().minus(Duration.ofDays(11)),
                        Instant.now().minus(Duration.ofDays(15)),
                        location,
                        managerUser,
                        null,
                        Set.of());
        pastEvent.id = id(101);

        Seat seat = new Seat("S1", location, "1", 1, 1, null, null);
        seat.id = id(200);

        reservation =
                new Reservation(
                        regularUser,
                        upcomingEvent,
                        seat,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        null);
        reservation.id = id(300);

        Seat blockedSeat = new Seat("S2", location, "1", 1, 2, null, null);
        blockedSeat.id = id(201);

        blockedReservation =
                new Reservation(
                        null,
                        upcomingEvent,
                        blockedSeat,
                        Instant.now(),
                        ReservationStatus.BLOCKED,
                        null);
        blockedReservation.id = id(301);

        allowance = new EventUserAllowance(regularUser, upcomingEvent, 3);
        allowance.id = id(400);
    }

    @Test
    void getOverview_AsManager_Success() {
        when(eventRepository.findByManager(managerUser))
                .thenReturn(List.of(upcomingEvent, pastEvent));
        when(reservationRepository.findByManager(managerUser))
                .thenReturn(List.of(reservation, blockedReservation));
        when(eventUserAllowanceRepository.findByEventManager(managerUser))
                .thenReturn(List.of(allowance));
        when(eventLocationRepository.getSeatCountsByLocationIds(Set.of(location.id)))
                .thenReturn(Map.of(location.id, 10));

        ManagementOverviewDTO overview = overviewService.getOverview(managerAuth);

        assertNotNull(overview);
        assertEquals(2, overview.stats().eventsCount());
        assertEquals(1, overview.stats().upcomingEventsCount());
        assertEquals(1, overview.stats().bookingOpenCount());
        assertEquals(2, overview.stats().reservationsCount());
        assertEquals(1, overview.stats().reservationsReserved());
        assertEquals(1, overview.stats().reservationsBlocked());
        assertEquals(0, overview.stats().reservationsPending());
        assertEquals(10, overview.stats().occupancyPercent());
        assertEquals(1, overview.stats().occupancyReserved());
        assertEquals(10, overview.stats().occupancyCapacity());
        assertEquals(
                25,
                overview.stats()
                        .contingentUsagePercent()); // 1 used out of (3 granted + 1 used = 4) => 25%
        assertEquals(1, overview.stats().contingentUsed());
        assertEquals(4, overview.stats().contingentGranted());

        assertEquals(1, overview.upcomingEvents().size());
        assertEquals("Future Event", overview.upcomingEvents().getFirst().name());
        assertEquals(1, overview.deadlineWarnings().size());
    }

    @Test
    void getOverview_AsAdmin_Success() {
        when(eventRepository.listAll()).thenReturn(List.of(upcomingEvent));
        when(reservationRepository.listAll()).thenReturn(List.of(reservation));
        when(eventUserAllowanceRepository.listAll()).thenReturn(List.of(allowance));
        when(eventLocationRepository.getSeatCountsByLocationIds(Set.of(location.id)))
                .thenReturn(Map.of(location.id, 10));

        ManagementOverviewDTO overview = overviewService.getOverview(adminAuth);

        assertNotNull(overview);
        assertEquals(1, overview.stats().eventsCount());
        verify(eventRepository).listAll();
    }
}
