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
package de.felixhertweck.seatreservation.email;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.email.service.EmailSeatMapService;
import de.felixhertweck.seatreservation.model.entity.EmailSeatMapToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EmailSeatMapServiceTest {

    @Inject EmailSeatMapService emailSeatMapService;

    @InjectMock EmailSeatMapTokenRepository tokenRepository;

    private User testUser;
    private Event testEvent;
    private EventLocation testLocation;
    private List<Reservation> testReservations;
    private Seat testSeat;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        testUser = new User();
        testUser.id = 1L;
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testLocation = new EventLocation();
        testLocation.id = 100L;
        testLocation.setName("Test Location");
        testLocation.setCapacity(50);

        testSeat = new Seat();
        testSeat.id = 1000L;
        testSeat.setSeatNumber("A1");
        testSeat.setSeatRow("A");
        testSeat.setLocation(testLocation);

        testLocation.setSeats(Collections.singletonList(testSeat));

        testEvent = new Event();
        testEvent.id = 10L;
        testEvent.setName("Test Event");
        testEvent.setStartTime(Instant.now().plus(7, ChronoUnit.DAYS));
        testEvent.setEndTime(Instant.now().plus(7, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
        testEvent.setEventLocation(testLocation);

        Reservation reservation = new Reservation();
        reservation.id = 10000L;
        reservation.setUser(testUser);
        reservation.setEvent(testEvent);
        reservation.setSeat(testSeat);
        reservation.setStatus(ReservationStatus.RESERVED);

        testReservations = Collections.singletonList(reservation);
        testUser.setReservations(new HashSet<>(testReservations));
    }

    @Test
    void createEmailSeatMapToken_Success() {
        doNothing().when(tokenRepository).persist(any(EmailSeatMapToken.class));

        String token =
                emailSeatMapService.createEmailSeatMapToken(testUser, testEvent, testReservations);

        assertNotNull(token);
        assertFalse(token.isBlank());
        verify(tokenRepository, times(1)).persist(any(EmailSeatMapToken.class));
    }

    @Test
    void createEmailSeatMapToken_GeneratesUniqueToken() {
        doNothing().when(tokenRepository).persist(any(EmailSeatMapToken.class));

        String token1 =
                emailSeatMapService.createEmailSeatMapToken(testUser, testEvent, testReservations);
        String token2 =
                emailSeatMapService.createEmailSeatMapToken(testUser, testEvent, testReservations);

        assertNotEquals(token1, token2, "Tokens should be unique");
    }

    @Test
    void createEmailSeatMapToken_WithMultipleReservations() {
        Seat seat2 = new Seat();
        seat2.id = 1001L;
        seat2.setSeatNumber("A2");
        seat2.setLocation(testLocation);

        Reservation reservation2 = new Reservation();
        reservation2.id = 10001L;
        reservation2.setUser(testUser);
        reservation2.setEvent(testEvent);
        reservation2.setSeat(seat2);

        List<Reservation> multipleReservations =
                Arrays.asList(testReservations.get(0), reservation2);

        doNothing().when(tokenRepository).persist(any(EmailSeatMapToken.class));

        String token =
                emailSeatMapService.createEmailSeatMapToken(
                        testUser, testEvent, multipleReservations);

        assertNotNull(token);
        verify(tokenRepository, times(1)).persist(any(EmailSeatMapToken.class));
    }

    @Test
    void createEmailSeatMapToken_WithEmptyReservations() {
        doNothing().when(tokenRepository).persist(any(EmailSeatMapToken.class));

        String token =
                emailSeatMapService.createEmailSeatMapToken(
                        testUser, testEvent, Collections.emptyList());

        assertNotNull(token);
        verify(tokenRepository, times(1)).persist(any(EmailSeatMapToken.class));
    }

    @Test
    void getSvgImage_Success_WithValidToken() {
        String token = UUID.randomUUID().toString();
        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(testEvent);
        emailSeatMapToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        emailSeatMapToken.setNewReservedSeatNumbers(Collections.singleton("A1"));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<String> svg = emailSeatMapService.getSvgImage(token);

        assertTrue(svg.isPresent());
        assertTrue(svg.get().contains("<svg"));
    }

    @Test
    void getSvgImage_ReturnsEmpty_WhenTokenNotFound() {
        String token = UUID.randomUUID().toString();
        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        Optional<String> svg = emailSeatMapService.getSvgImage(token);

        assertFalse(svg.isPresent());
    }

    @Test
    void getSvgImage_ReturnsEmpty_WhenTokenExpired() {
        String token = UUID.randomUUID().toString();
        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(testEvent);
        emailSeatMapToken.setExpirationTime(Instant.now().minus(1, ChronoUnit.DAYS));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<String> svg = emailSeatMapService.getSvgImage(token);

        assertFalse(svg.isPresent());
    }

    @Test
    void getSvgImage_ReturnsEmpty_WhenTokenIsNull() {
        Optional<String> svg = emailSeatMapService.getSvgImage(null);

        assertFalse(svg.isPresent());
        verify(tokenRepository, never()).findByToken(anyString());
    }

    @Test
    void getSvgImage_ReturnsEmpty_WhenTokenIsBlank() {
        Optional<String> svg = emailSeatMapService.getSvgImage("");

        assertFalse(svg.isPresent());
        verify(tokenRepository, never()).findByToken(anyString());
    }

    @Test
    void getSvgImage_ReturnsEmpty_WhenEventIsNull() {
        String token = UUID.randomUUID().toString();
        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(null);
        emailSeatMapToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<String> svg = emailSeatMapService.getSvgImage(token);

        assertFalse(svg.isPresent());
    }

    @Test
    void getSvgImage_ReturnsEmpty_WhenEventLocationIsNull() {
        String token = UUID.randomUUID().toString();
        Event eventWithoutLocation = new Event();
        eventWithoutLocation.id = 10L;
        eventWithoutLocation.setName("Test Event");
        eventWithoutLocation.setEventLocation(null);

        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(eventWithoutLocation);
        emailSeatMapToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<String> svg = emailSeatMapService.getSvgImage(token);

        assertFalse(svg.isPresent());
    }

    @Test
    void getSvgImage_WithExistingReservations() {
        String token = UUID.randomUUID().toString();

        // Add existing reservation
        Seat existingSeat = new Seat();
        existingSeat.id = 1001L;
        existingSeat.setSeatNumber("B1");
        existingSeat.setSeatRow("B");
        existingSeat.setLocation(testLocation);

        Reservation existingReservation = new Reservation();
        existingReservation.id = 10001L;
        existingReservation.setUser(testUser);
        existingReservation.setEvent(testEvent);
        existingReservation.setSeat(existingSeat);

        testUser.getReservations().add(existingReservation);
        testLocation.setSeats(Arrays.asList(testSeat, existingSeat));

        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(testEvent);
        emailSeatMapToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        emailSeatMapToken.setNewReservedSeatNumbers(Collections.singleton("A1"));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<String> svg = emailSeatMapService.getSvgImage(token);

        assertTrue(svg.isPresent());
        assertTrue(svg.get().contains("<svg"));
    }

    @Test
    void getSvgImage_WithMarkers() {
        String token = UUID.randomUUID().toString();

        EventLocationMarker marker = new EventLocationMarker();
        marker.setLabel("Stage");
        marker.setxCoordinate(100);
        marker.setyCoordinate(50);
        marker.setEventLocation(testLocation);

        testLocation.setMarkers(Collections.singletonList(marker));

        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(testEvent);
        emailSeatMapToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        emailSeatMapToken.setNewReservedSeatNumbers(Collections.singleton("A1"));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<String> svg = emailSeatMapService.getSvgImage(token);

        assertTrue(svg.isPresent());
        assertTrue(svg.get().contains("<svg"));
    }

    @Test
    void getPngImage_Success_WithValidToken() {
        String token = UUID.randomUUID().toString();
        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(testEvent);
        emailSeatMapToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        emailSeatMapToken.setNewReservedSeatNumbers(Collections.singleton("A1"));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<byte[]> png = emailSeatMapService.getPngImage(token);

        assertTrue(png.isPresent());
        assertTrue(png.get().length > 0);
    }

    @Test
    void getPngImage_ReturnsEmpty_WhenTokenNotFound() {
        String token = UUID.randomUUID().toString();
        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        Optional<byte[]> png = emailSeatMapService.getPngImage(token);

        assertFalse(png.isPresent());
    }

    @Test
    void getPngImage_ReturnsEmpty_WhenTokenExpired() {
        String token = UUID.randomUUID().toString();
        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(testEvent);
        emailSeatMapToken.setExpirationTime(Instant.now().minus(1, ChronoUnit.DAYS));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<byte[]> png = emailSeatMapService.getPngImage(token);

        assertFalse(png.isPresent());
    }

    @Test
    void getSvgImage_WithNoExpirationTime() {
        String token = UUID.randomUUID().toString();
        EmailSeatMapToken emailSeatMapToken = new EmailSeatMapToken();
        emailSeatMapToken.setToken(token);
        emailSeatMapToken.setUser(testUser);
        emailSeatMapToken.setEvent(testEvent);
        emailSeatMapToken.setExpirationTime(null);
        emailSeatMapToken.setNewReservedSeatNumbers(Collections.singleton("A1"));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(emailSeatMapToken));

        Optional<String> svg = emailSeatMapService.getSvgImage(token);

        assertTrue(svg.isPresent(), "Should return SVG when expiration time is null");
    }
}
