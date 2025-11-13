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
package de.felixhertweck.seatreservation.supervisor.service;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.supervisor.exception.InvalidEventIdException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.WebSocketConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class LiveViewServiceTest {

    @Inject LiveViewService webSocketService;

    @InjectMock ReservationRepository reservationRepository;

    @InjectMock EventRepository eventRepository;

    private final Long eventId = 1L;

    @BeforeEach
    public void setUp() {
        Mockito.reset(reservationRepository);
        Mockito.reset(eventRepository);
    }

    @Test
    void testGetActiveConnectionCount_NoConnections() {
        int count = webSocketService.getActiveConnectionCount(eventId);
        assertEquals(0, count, "Should have 0 connections initially");
    }

    @Test
    void testBroadcastCheckInUpdate_NoActiveConnections() {
        // Should not throw exception when no connections are active
        Reservation reservation = createTestReservation();

        assertDoesNotThrow(
                () -> {
                    webSocketService.broadcastUpdate(eventId, reservation);
                });
    }

    @Test
    void testRegisterConnection_StringEventId_Success() {
        // Should successfully parse valid event ID string and register connection
        String validEventIdStr = "123";
        Long parsedEventId = 123L;
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);

        // Mock event and location
        Event mockEvent = new Event();
        mockEvent.id = parsedEventId;
        mockEvent.setName("Test Event");
        EventLocation mockLocation = new EventLocation();
        mockLocation.id = 1L;
        mockEvent.setEventLocation(mockLocation);

        Mockito.when(eventRepository.findById(parsedEventId)).thenReturn(mockEvent);
        Mockito.when(reservationRepository.findByEventId(parsedEventId))
                .thenReturn(new java.util.ArrayList<>());

        assertDoesNotThrow(
                () -> {
                    webSocketService.registerConnection(validEventIdStr, mockConnection);
                });
    }

    @Test
    void testUnregisterConnection_StringEventId_Success() {
        // Should successfully parse valid event ID string and unregister connection
        String validEventIdStr = "456";
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);
        assertDoesNotThrow(
                () -> {
                    webSocketService.unregisterConnection(validEventIdStr, mockConnection);
                });
    }

    @Test
    void testRegisterConnection_InvalidEventId_NegativeNumber() {
        // Should throw InvalidEventIdException for negative event ID
        String invalidEventIdStr = "-123";
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);
        assertThrows(
                InvalidEventIdException.class,
                () -> webSocketService.registerConnection(invalidEventIdStr, mockConnection));
    }

    @Test
    void testRegisterConnection_InvalidEventId_NotANumber() {
        // Should throw InvalidEventIdException for non-numeric event ID
        String invalidEventIdStr = "abc";
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);
        assertThrows(
                InvalidEventIdException.class,
                () -> webSocketService.registerConnection(invalidEventIdStr, mockConnection));
    }

    @Test
    void testRegisterConnection_InvalidEventId_Blank() {
        // Should throw InvalidEventIdException for blank event ID
        String blankEventIdStr = "   ";
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);
        assertThrows(
                InvalidEventIdException.class,
                () -> webSocketService.registerConnection(blankEventIdStr, mockConnection));
    }

    @Test
    void testRegisterConnection_InvalidEventId_Null() {
        // Should throw InvalidEventIdException for null event ID
        String nullEventIdStr = null;
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);
        assertThrows(
                InvalidEventIdException.class,
                () -> webSocketService.registerConnection(nullEventIdStr, mockConnection));
    }

    private Reservation createTestReservation() {
        User user = new User();
        user.id = 1L;
        user.setUsername("testuser");

        Event event = new Event();
        event.id = eventId;
        event.setName("Test Event");

        EventLocation location = new EventLocation();
        location.id = 1L;

        Seat seat = new Seat();
        seat.id = 1L;
        seat.setSeatNumber("A1");
        seat.setLocation(location);

        Reservation reservation = new Reservation();
        reservation.id = 1L;
        reservation.setUser(user);
        reservation.setEvent(event);
        reservation.setSeat(seat);
        reservation.setLiveStatus(ReservationLiveStatus.CHECKED_IN);

        return reservation;
    }
}
