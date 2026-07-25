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

import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EventLocationServiceDeleteTest {

    @Mock EventLocationRepository eventLocationRepository;

    @InjectMocks EventLocationService eventLocationService;

    private User adminUser;
    private User managerUser;
    private User otherUser;
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser otherAuth;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        adminUser = new User();
        adminUser.id = id(1);
        adminUser.setRoles(Set.of(Roles.ADMIN));

        managerUser = new User();
        managerUser.id = id(2);
        managerUser.setRoles(Set.of(Roles.MANAGER));

        otherUser = new User();
        otherUser.id = id(3);
        otherUser.setRoles(Set.of(Roles.USER));

        adminAuth = new AuthenticatedUser(adminUser.id, adminUser.getRoles());
        managerAuth = new AuthenticatedUser(managerUser.id, managerUser.getRoles());
        otherAuth = new AuthenticatedUser(otherUser.id, otherUser.getRoles());
    }

    @Test
    void deleteEventLocation_Success_AsManager() {
        EventLocation loc1 = new EventLocation("Hall 1", "Address 1", managerUser, 100);
        loc1.id = id(101);

        EventLocation loc2 = new EventLocation("Hall 2", "Address 2", managerUser, 200);
        loc2.id = id(102);

        PanacheQuery<EventLocation> queryMock = mock(PanacheQuery.class);
        when(queryMock.list()).thenReturn(List.of(loc1, loc2));
        when(eventLocationRepository.find(
                        eq("from EventLocation el left join fetch el.manager where el.id in ?1"),
                        eq(List.of(id(101), id(102)))))
                .thenReturn(queryMock);

        eventLocationService.deleteEventLocation(List.of(id(101), id(102)), managerAuth);

        verify(eventLocationRepository, times(1)).delete(loc1);
        verify(eventLocationRepository, times(1)).delete(loc2);
    }

    @Test
    void deleteEventLocation_NotFound_ThrowsAndDoesNotDelete() {
        EventLocation loc1 = new EventLocation("Hall 1", "Address 1", managerUser, 100);
        loc1.id = id(101);

        PanacheQuery<EventLocation> queryMock = mock(PanacheQuery.class);
        when(queryMock.list()).thenReturn(List.of(loc1));
        when(eventLocationRepository.find(
                        eq("from EventLocation el left join fetch el.manager where el.id in ?1"),
                        eq(List.of(id(101), id(999)))))
                .thenReturn(queryMock);

        assertThrows(
                EventLocationNotFoundException.class,
                () ->
                        eventLocationService.deleteEventLocation(
                                List.of(id(101), id(999)), managerAuth));

        verify(eventLocationRepository, never()).delete(any());
    }

    @Test
    void deleteEventLocation_Forbidden_ThrowsAndDoesNotDelete() {
        EventLocation loc1 = new EventLocation("Hall 1", "Address 1", managerUser, 100);
        loc1.id = id(101);

        PanacheQuery<EventLocation> queryMock = mock(PanacheQuery.class);
        when(queryMock.list()).thenReturn(List.of(loc1));
        when(eventLocationRepository.find(
                        eq("from EventLocation el left join fetch el.manager where el.id in ?1"),
                        eq(List.of(id(101)))))
                .thenReturn(queryMock);

        assertThrows(
                SecurityException.class,
                () -> eventLocationService.deleteEventLocation(List.of(id(101)), otherAuth));

        verify(eventLocationRepository, never()).delete(any());
    }
}
