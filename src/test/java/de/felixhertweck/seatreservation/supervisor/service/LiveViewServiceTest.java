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

    private final long eventId = 1L;

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
        long parsedEventId = 123L;
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

    @Test
    void testSendInitialReservations_IOException() throws Exception {
        // webSocketService is a CDI client proxy; its own fields are never read by the delegated
        // business methods, so the objectMapper field must be patched on the real contextual
        // instance behind the proxy, not on the proxy itself.
        Object realInstance = io.quarkus.arc.ClientProxy.unwrap(webSocketService);

        java.lang.reflect.Field mapperField =
                LiveViewService.class.getDeclaredField("objectMapper");
        mapperField.setAccessible(true);
        com.fasterxml.jackson.databind.ObjectMapper originalMapper =
                (com.fasterxml.jackson.databind.ObjectMapper) mapperField.get(realInstance);

        com.fasterxml.jackson.databind.ObjectMapper mockMapper =
                Mockito.mock(com.fasterxml.jackson.databind.ObjectMapper.class);
        Mockito.when(mockMapper.writeValueAsString(Mockito.any()))
                .thenThrow(
                        new com.fasterxml.jackson.core.JsonProcessingException(
                                "Test IOException") {});

        mapperField.set(realInstance, mockMapper);

        try {
            Long testEventId = 999L;
            WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);

            Event mockEvent = new Event();
            mockEvent.id = testEventId;
            mockEvent.setName("Test Exception Event");
            EventLocation mockLocation = new EventLocation();
            mockLocation.id = 1L;
            mockEvent.setEventLocation(mockLocation);

            Mockito.when(eventRepository.findById(testEventId)).thenReturn(mockEvent);
            Mockito.when(reservationRepository.findByEventId(testEventId))
                    .thenReturn(new java.util.ArrayList<>());

            assertDoesNotThrow(
                    () -> {
                        webSocketService.registerConnection(testEventId, mockConnection);
                    },
                    "registerConnection should handle IOException without throwing it");

            // Verify that writeValueAsString was indeed called
            Mockito.verify(mockMapper).writeValueAsString(Mockito.any());
        } finally {
            // Restore original mapper
            mapperField.set(webSocketService, originalMapper);
        }
    }

    @Test
    void testBroadcastUpdate_IOExceptionHandledAndConnectionRemoved() throws Exception {
        Long testEventId = 100L;
        Reservation mockReservation = new Reservation();
        mockReservation.id = 1L;

        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);

        // Register the mock connection for the event
        webSocketService.registerConnection(testEventId, mockConnection);

        // Unwrap the CDI proxy to get the real instance
        Object realInstance = io.quarkus.arc.ClientProxy.unwrap(webSocketService);

        // Save original mapper
        java.lang.reflect.Field mapperField =
                LiveViewService.class.getDeclaredField("objectMapper");
        mapperField.setAccessible(true);
        com.fasterxml.jackson.databind.ObjectMapper originalMapper =
                (com.fasterxml.jackson.databind.ObjectMapper) mapperField.get(realInstance);

        // Mock the mapper to throw a JsonProcessingException (which is a subclass of IOException)
        com.fasterxml.jackson.databind.ObjectMapper mockMapper =
                Mockito.mock(com.fasterxml.jackson.databind.ObjectMapper.class);
        Mockito.when(mockMapper.writeValueAsString(Mockito.any()))
                .thenThrow(
                        new com.fasterxml.jackson.core.JsonProcessingException(
                                "Simulated IOException") {});

        // Inject the mock mapper
        mapperField.set(realInstance, mockMapper);

        try {
            // Invoke the method under test
            assertDoesNotThrow(
                    () -> webSocketService.broadcastUpdate(testEventId, mockReservation));

            // Verify that the exception was caught and the connection was removed
            // getActiveConnectionCount should return 0 since the failed connection was removed
            assertEquals(0, webSocketService.getActiveConnectionCount(testEventId));
        } finally {
            // Restore original mapper
            mapperField.set(realInstance, originalMapper);
            webSocketService.unregisterConnection(testEventId, mockConnection); // cleanup
        }
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

        Seat seat = new Seat("A1", "", location);
        seat.id = 1L;

        Reservation reservation = new Reservation();
        reservation.id = 1L;
        reservation.setUser(user);
        reservation.setEvent(event);
        reservation.setSeat(seat);
        reservation.setLiveStatus(ReservationLiveStatus.CHECKED_IN);

        return reservation;
    }
}
