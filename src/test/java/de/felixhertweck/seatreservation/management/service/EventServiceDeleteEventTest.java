/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.common.events.EventDeletedEvent;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EventServiceDeleteEventTest {

    @Mock EventRepository eventRepository;
    @Mock ReservationRepository reservationRepository;

    @Mock jakarta.enterprise.event.Event<EventDeletedEvent> eventDeletedBus;

    @InjectMocks EventService eventService;

    User adminUser;
    User managerUser;
    User otherUser;
    User coManagerUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        EventAccessService eventAccessService = new EventAccessService();
        eventAccessService.eventRepository = eventRepository;
        eventService.eventAccessService = eventAccessService;
        eventService.reservationRepository = reservationRepository;

        adminUser = new User();
        adminUser.id = id(1);
        adminUser.setRoles(Set.of(Roles.ADMIN));

        managerUser = new User();
        managerUser.id = id(2);
        managerUser.setRoles(Set.of(Roles.USER));

        otherUser = new User();
        otherUser.id = id(3);
        otherUser.setRoles(Set.of(Roles.USER));

        coManagerUser = new User();
        coManagerUser.id = id(4);
        coManagerUser.setRoles(Set.of(Roles.USER));
    }

    @Test
    void deleteEvent_Success() {
        Event event1 = new Event();
        event1.id = id(101);
        event1.setName("Event 1");
        event1.setManager(managerUser);

        Event event2 = new Event();
        event2.id = id(102);
        event2.setName("Event 2");
        event2.setManager(managerUser);

        when(eventRepository.findByIdsWithManager(eq(List.of(id(101), id(102)))))
                .thenReturn(List.of(event1, event2));
        stubIsUserManager(event1, event2);

        eventService.deleteEvent(List.of(id(101), id(102)), managerUser);

        verify(eventRepository, times(1)).delete(event1);
        verify(eventRepository, times(1)).delete(event2);
    }

    @Test
    void deleteEvent_Success_AsCoManager() {
        // coManagerUser is added as a co-manager, not via setManager (which would make it the sole
        // creator) - proves deletion isn't limited to whoever created the event.
        Event event1 = new Event();
        event1.id = id(101);
        event1.setName("Event 1");
        event1.setManager(managerUser);
        event1.getManagers().add(coManagerUser);

        when(eventRepository.findByIdsWithManager(eq(List.of(id(101)))))
                .thenReturn(List.of(event1));
        stubIsUserManager(event1);

        eventService.deleteEvent(List.of(id(101)), coManagerUser);

        verify(eventRepository, times(1)).delete(event1);
    }

    @Test
    void deleteEvent_NotFound_ThrowsAndDoesNotDeleteAny() {
        Event event1 = new Event();
        event1.id = id(101);
        event1.setName("Event 1");
        event1.setManager(managerUser);

        when(eventRepository.findByIdsWithManager(eq(List.of(id(101), id(999)))))
                .thenReturn(List.of(event1));
        stubIsUserManager(event1);

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.deleteEvent(List.of(id(101), id(999)), managerUser));

        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deleteEvent_NotAuthorized_ThrowsAndDoesNotDelete() {
        Event event1 = new Event();
        event1.id = id(101);
        event1.setName("Event 1");
        event1.setManager(managerUser);

        when(eventRepository.findByIdsWithManager(eq(List.of(id(101)))))
                .thenReturn(List.of(event1));

        assertThrows(
                AccessDeniedException.class,
                () -> eventService.deleteEvent(List.of(id(101)), otherUser));

        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deleteEvent_WithActiveReservations_ThrowsValidationExceptionAndDoesNotDelete() {
        Event event1 = new Event();
        event1.id = id(101);
        event1.setName("Event 1");
        event1.setManager(managerUser);

        when(eventRepository.findByIdsWithManager(eq(List.of(id(101)))))
                .thenReturn(List.of(event1));
        stubIsUserManager(event1);
        when(reservationRepository.countActiveByEvent(event1)).thenReturn(3L);

        ValidationException ex =
                assertThrows(
                        ValidationException.class,
                        () -> eventService.deleteEvent(List.of(id(101)), managerUser));

        assertTrue(ex.getMessage().contains("aktive Reservierungen"));
        verify(eventRepository, never()).delete(any());
    }

    private void stubIsUserManager(Event... events) {
        when(eventRepository.isUserManager(any(UUID.class), any(UUID.class)))
                .thenAnswer(
                        invocation -> {
                            UUID eventId = invocation.getArgument(0);
                            UUID userId = invocation.getArgument(1);
                            for (Event event : events) {
                                if (event.getId().equals(eventId)) {
                                    return event.getManagers() != null
                                            && event.getManagers().stream()
                                                    .anyMatch(u -> u.id.equals(userId));
                                }
                            }
                            return false;
                        });
    }
}
