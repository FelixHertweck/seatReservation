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
package de.felixhertweck.seatreservation.email.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.felixhertweck.seatreservation.model.entity.Coordinate;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.utils.CodeGenerator;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link NotificationService#loadReminderData(Long)} against a real Hibernate session,
 * unlike the Mockito-based {@code NotificationServiceTest}: entities here are genuine, lazily
 * loaded proxies, and {@code loadReminderData} genuinely commits and closes its own transaction
 * before returning. That is what makes it possible to actually reproduce the failure this test
 * guards against: {@code sendReminderEmails} (see its {@code @ActivateRequestContext} javadoc)
 * reads {@code event.getEventLocation()}'s seats/markers/areas after that transaction has already
 * committed. Without {@code eagerLoadEntities} force-loading those collections, touching them here
 * — outside any transaction, exactly like the real reminder-sending path does — throws
 * LazyInitializationException.
 */
@QuarkusTest
class NotificationServiceReminderDataTest {

    @Inject NotificationService notificationService;

    @Inject UserRepository userRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject EventLocationMarkerRepository markerRepository;
    @Inject EventLocationAreaRepository areaRepository;
    @Inject EventLocationEntranceRepository entranceRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventRepository eventRepository;
    @Inject ReservationRepository reservationRepository;

    private UUID eventId;

    @BeforeEach
    @Transactional
    void setUp() {
        cleanUpDatabase();

        User manager = userRepository.findByUsernameOptional("manager").orElseThrow();
        User attendee = userRepository.findByUsernameOptional("user").orElseThrow();

        EventLocation location =
                new EventLocation("Reminder Test Hall", "Test Street 1", manager, 10);
        eventLocationRepository.persist(location);

        Seat seat = new Seat("A1", location, "A", 1, 1, null, null);
        seatRepository.persist(seat);
        location.setSeats(List.of(seat));

        EventLocationMarker marker = new EventLocationMarker("Entrance", 0, 0);
        marker.setEventLocation(location);
        markerRepository.persist(marker);
        location.setMarkers(List.of(marker));

        EventLocationArea area = new EventLocationArea("VIP");
        area.setEventLocation(location);
        area.setBoundary(List.of(new Coordinate(0, 0), new Coordinate(5, 0), new Coordinate(5, 5)));
        areaRepository.persist(area);
        location.setAreas(List.of(area));

        Event event =
                new Event(
                        "Reminder Test Event",
                        "Description",
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        Instant.now().plus(1, ChronoUnit.DAYS).plusSeconds(3600),
                        Instant.now().minus(1, ChronoUnit.DAYS),
                        Instant.now().minus(2, ChronoUnit.DAYS),
                        location,
                        manager,
                        Instant.now().minusSeconds(60),
                        null);
        eventRepository.persist(event);
        eventId = event.id;

        Reservation reservation =
                new Reservation(
                        attendee,
                        event,
                        seat,
                        Instant.now(),
                        ReservationStatus.RESERVED,
                        CodeGenerator.generateRandomCode());
        reservationRepository.persist(reservation);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        cleanUpDatabase();
    }

    private void cleanUpDatabase() {
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        areaRepository.deleteAll();
        entranceRepository.deleteAll();
        markerRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    /**
     * The core regression test: loadReminderData()'s own @Transactional method has already
     * committed by the time this assertion runs, so reading the location's seats/markers/areas/
     * boundary here is exactly the situation sendReminderEmails() is in for real. Remove the
     * Hibernate.initialize(...) calls from eagerLoadEntities() to see this fail with
     * LazyInitializationException.
     */
    @Test
    void loadReminderData_LocationCollectionsSurviveOutsideTheTransaction() {
        NotificationService.ReminderData data =
                assertDoesNotThrow(() -> notificationService.loadReminderData(eventId));

        EventLocation location = data.event.getEventLocation();
        assertDoesNotThrow(
                () -> {
                    assertEquals(1, location.getSeats().size());
                    assertEquals(1, location.getMarkers().size());
                    assertEquals(1, location.getAreas().size());
                    assertEquals(3, location.getAreas().get(0).getBoundary().size());
                });
    }
}
