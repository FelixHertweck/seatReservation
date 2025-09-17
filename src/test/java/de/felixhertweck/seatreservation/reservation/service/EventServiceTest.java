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
package de.felixhertweck.seatreservation.reservation.service;

import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.dto.EventResponseDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventServiceTest {

    @Inject EventService eventService;

    @InjectMock UserRepository userRepository;

    @InjectMock EventUserAllowanceRepository eventUserAllowanceRepository;

    private User user;
    private Event event;
    private EventUserAllowance allowance;

    @BeforeEach
    void setUp() {
        user = new User();
        user.id = 1L;
        user.setUsername("testuser");

        var location = new de.felixhertweck.seatreservation.model.entity.EventLocation();
        location.id = 1L;

        event = new Event();
        event.id = 1L;
        event.setEventLocation(location);

        allowance = new EventUserAllowance();
        allowance.setUser(user);
        allowance.setEvent(event);
        allowance.setReservationsAllowedCount(5);
    }

    @Test
    void getEventsForCurrentUser_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(List.of(allowance));

        List<EventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(event.id, result.getFirst().id());
        assertEquals(5, result.getFirst().reservationsAllowed());
    }

    @Test
    void getEventsForCurrentUser_UserNotFoundException() {
        when(userRepository.findByUsername("unknownuser")).thenReturn(null);

        assertThrows(
                UserNotFoundException.class,
                () -> eventService.getEventsForCurrentUser("unknownuser"));
    }

    @Test
    void getEventsForCurrentUser_Success_NoEvents() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(eventUserAllowanceRepository.findByUser(user)).thenReturn(Collections.emptyList());

        List<EventResponseDTO> result = eventService.getEventsForCurrentUser("testuser");

        assertTrue(result.isEmpty());
    }
}
