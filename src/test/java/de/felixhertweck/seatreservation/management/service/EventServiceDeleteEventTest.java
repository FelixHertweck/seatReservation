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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EventServiceDeleteEventTest {

    @Mock EventRepository eventRepository;

    @InjectMocks EventService eventService;

    User adminUser;
    User managerUser;
    User otherUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        adminUser = new User();
        adminUser.id = id(1);
        adminUser.setRoles(Set.of(Roles.ADMIN));

        managerUser = new User();
        managerUser.id = id(2);
        managerUser.setRoles(Set.of(Roles.USER));

        otherUser = new User();
        otherUser.id = id(3);
        otherUser.setRoles(Set.of(Roles.USER));
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

        PanacheQuery<Event> queryMock = mock(PanacheQuery.class);
        when(queryMock.list()).thenReturn(List.of(event1, event2));
        when(eventRepository.find(
                        eq("from Event e left join fetch e.manager where e.id in ?1"),
                        eq(List.of(id(101), id(102)))))
                .thenReturn(queryMock);

        eventService.deleteEvent(List.of(id(101), id(102)), managerUser);

        verify(eventRepository, times(1)).delete(event1);
        verify(eventRepository, times(1)).delete(event2);
    }

    @Test
    void deleteEvent_NotFound_ThrowsAndDoesNotDeleteAny() {
        Event event1 = new Event();
        event1.id = id(101);
        event1.setName("Event 1");
        event1.setManager(managerUser);

        PanacheQuery<Event> queryMock = mock(PanacheQuery.class);
        when(queryMock.list()).thenReturn(List.of(event1));
        when(eventRepository.find(
                        eq("from Event e left join fetch e.manager where e.id in ?1"),
                        eq(List.of(id(101), id(999)))))
                .thenReturn(queryMock);

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

        PanacheQuery<Event> queryMock = mock(PanacheQuery.class);
        when(queryMock.list()).thenReturn(List.of(event1));
        when(eventRepository.find(
                        eq("from Event e left join fetch e.manager where e.id in ?1"),
                        eq(List.of(id(101)))))
                .thenReturn(queryMock);

        assertThrows(
                SecurityException.class,
                () -> eventService.deleteEvent(List.of(id(101)), otherUser));

        verify(eventRepository, never()).delete(any());
    }
}
