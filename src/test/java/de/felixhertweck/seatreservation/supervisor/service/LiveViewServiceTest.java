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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.BoxOfficeGuestInfo;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.BoxOfficeGuestInfoRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.supervisor.exception.InvalidEventIdException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.WebSocketConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@QuarkusTest
public class LiveViewServiceTest {

    @Inject LiveViewService webSocketService;

    @InjectMock ReservationRepository reservationRepository;

    @InjectMock EventRepository eventRepository;

    @InjectMock BoxOfficeGuestInfoRepository boxOfficeGuestInfoRepository;

    private final UUID eventId = id(1);

    @BeforeEach
    public void setUp() {
        Mockito.reset(reservationRepository);
        Mockito.reset(eventRepository);
        Mockito.reset(boxOfficeGuestInfoRepository);
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
        UUID parsedEventId = id(123);
        String validEventIdStr = parsedEventId.toString();
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);

        // Mock event and location
        Event mockEvent = new Event();
        mockEvent.id = parsedEventId;
        mockEvent.setName("Test Event");
        EventLocation mockLocation = new EventLocation();
        mockLocation.id = id(1);
        mockEvent.setEventLocation(mockLocation);

        Mockito.when(eventRepository.findById(parsedEventId)).thenReturn(mockEvent);
        Mockito.when(reservationRepository.findByEventIdWithUserAndSeat(parsedEventId))
                .thenReturn(new java.util.ArrayList<>());

        assertDoesNotThrow(
                () -> {
                    webSocketService.registerConnection(validEventIdStr, mockConnection);
                });
    }

    @Test
    void testUnregisterConnection_StringEventId_Success() {
        // Should successfully parse valid event ID string and unregister connection
        String validEventIdStr = id(456).toString();
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
            UUID testEventId = id(999);
            WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);

            Event mockEvent = new Event();
            mockEvent.id = testEventId;
            mockEvent.setName("Test Exception Event");
            EventLocation mockLocation = new EventLocation();
            mockLocation.id = id(1);
            mockEvent.setEventLocation(mockLocation);

            Mockito.when(eventRepository.findById(testEventId)).thenReturn(mockEvent);
            Mockito.when(reservationRepository.findByEventIdWithUserAndSeat(testEventId))
                    .thenReturn(new java.util.ArrayList<>());

            assertDoesNotThrow(
                    () -> {
                        webSocketService.registerConnection(testEventId, mockConnection);
                    },
                    "registerConnection should handle IOException without throwing it");

            // Verify that writeValueAsString was indeed called
            Mockito.verify(mockMapper).writeValueAsString(Mockito.any());
        } finally {
            // Restore on realInstance, not the proxy -- the proxy has no field state of its own,
            // so setting it there is a no-op and previously left the mock mapper permanently
            // swapped into the shared singleton, leaking into every test that ran afterward.
            mapperField.set(realInstance, originalMapper);
        }
    }

    @Test
    void testBroadcastUpdate_IOExceptionHandledAndConnectionRemoved() throws Exception {
        UUID testEventId = id(100);
        Reservation mockReservation = createTestReservation();

        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);

        // registerConnection() sends an initial reservations snapshot, so the repositories
        // must be stubbed for testEventId or that call fails before broadcastUpdate runs.
        Event mockEvent = new Event();
        mockEvent.id = testEventId;
        mockEvent.setName("Test Broadcast Event");
        EventLocation mockLocation = new EventLocation();
        mockLocation.id = id(1);
        mockEvent.setEventLocation(mockLocation);
        Mockito.when(eventRepository.findById(testEventId)).thenReturn(mockEvent);
        Mockito.when(reservationRepository.findByEventIdWithUserAndSeat(testEventId))
                .thenReturn(new java.util.ArrayList<>());

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

    @Test
    void testBroadcastNewReservation_NoActiveConnections() {
        // Should not throw exception when no connections are active
        Reservation reservation = createTestReservation();

        assertDoesNotThrow(
                () -> webSocketService.broadcastNewReservation(eventId, reservation, "Jane Doe"));
    }

    @Test
    void testBroadcastNewReservation_SendsNewReservationMessageWithGuestName() throws Exception {
        UUID testEventId = id(200);
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);

        Event mockEvent = new Event();
        mockEvent.id = testEventId;
        mockEvent.setName("Test Broadcast Event");
        EventLocation mockLocation = new EventLocation();
        mockLocation.id = id(1);
        mockEvent.setEventLocation(mockLocation);
        Mockito.when(eventRepository.findById(testEventId)).thenReturn(mockEvent);
        Mockito.when(reservationRepository.findByEventIdWithUserAndSeat(testEventId))
                .thenReturn(new java.util.ArrayList<>());

        // registerConnection sends an initial snapshot (1st sendText call) before the
        // broadcastNewReservation under test triggers a 2nd one.
        webSocketService.registerConnection(testEventId, mockConnection);

        Reservation reservation = createTestReservation();
        reservation.setEvent(mockEvent);

        try {
            webSocketService.broadcastNewReservation(testEventId, reservation, "Jane Doe");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(mockConnection, Mockito.times(2)).sendText(captor.capture());

            String newReservationMessage = captor.getAllValues().get(1);
            assertTrue(newReservationMessage.contains("NEW_RESERVATION"));
            assertTrue(newReservationMessage.contains("Jane Doe"));
        } finally {
            webSocketService.unregisterConnection(testEventId, mockConnection);
        }
    }

    @Test
    void testSendInitialReservations_IncludesGuestNameForBoxOfficeReservation() throws Exception {
        UUID testEventId = id(300);
        WebSocketConnection mockConnection = Mockito.mock(WebSocketConnection.class);

        Event mockEvent = new Event();
        mockEvent.id = testEventId;
        mockEvent.setName("Test Guest Name Event");
        EventLocation mockLocation = new EventLocation();
        mockLocation.id = id(1);
        mockEvent.setEventLocation(mockLocation);

        Reservation reservation = createTestReservation();
        reservation.setEvent(mockEvent);

        Mockito.when(eventRepository.findById(testEventId)).thenReturn(mockEvent);
        Mockito.when(reservationRepository.findByEventIdWithUserAndSeat(testEventId))
                .thenReturn(java.util.List.of(reservation));

        BoxOfficeGuestInfo guestInfo = new BoxOfficeGuestInfo(reservation, "Jane Doe");
        Mockito.when(
                        boxOfficeGuestInfoRepository.findByReservationIdIn(
                                java.util.List.of(reservation.id)))
                .thenReturn(java.util.List.of(guestInfo));

        try {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

            webSocketService.registerConnection(testEventId, mockConnection);

            Mockito.verify(mockConnection).sendText(captor.capture());
            assertTrue(captor.getValue().contains("Jane Doe"));
        } finally {
            webSocketService.unregisterConnection(testEventId, mockConnection);
        }
    }

    private Reservation createTestReservation() {
        User user = new User();
        user.id = id(1);
        user.setUsername("testuser");

        Event event = new Event();
        event.id = eventId;
        event.setName("Test Event");

        EventLocation location = new EventLocation();
        location.id = id(1);

        Seat seat = new Seat("A1", "", location);
        seat.id = id(1);

        Reservation reservation = new Reservation();
        reservation.id = id(1);
        reservation.setUser(user);
        reservation.setEvent(event);
        reservation.setSeat(seat);
        reservation.setLiveStatus(ReservationLiveStatus.CHECKED_IN);

        return reservation;
    }
}
