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

package de.felixhertweck.seatreservation.wallet.service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.EmailSeatMapToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GoogleWalletServiceTest {

    @Inject GoogleWalletService googleWalletService;

    @InjectMock EmailSeatMapTokenRepository tokenRepository;

    @org.junit.jupiter.api.BeforeEach
    void setup(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        // Generate a temporary private key for testing
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair kp = kpg.generateKeyPair();
        String privateKeyContent =
                java.util.Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

        java.nio.file.Path keyFile = tempDir.resolve("privateKey.pem");
        java.nio.file.Files.writeString(
                keyFile,
                "-----BEGIN PRIVATE KEY-----\n"
                        + privateKeyContent
                        + "\n-----END PRIVATE KEY-----");

        // Inject properties using reflection
        java.lang.reflect.Field issuerIdField =
                GoogleWalletService.class.getDeclaredField("issuerId");
        issuerIdField.setAccessible(true);
        issuerIdField.set(googleWalletService, "test-issuer");

        java.lang.reflect.Field serviceAccountEmailField =
                GoogleWalletService.class.getDeclaredField("serviceAccountEmail");
        serviceAccountEmailField.setAccessible(true);
        serviceAccountEmailField.set(googleWalletService, "test-service-account@test.com");

        java.lang.reflect.Field keyLocationField =
                GoogleWalletService.class.getDeclaredField("keyLocation");
        keyLocationField.setAccessible(true);
        keyLocationField.set(googleWalletService, keyFile.toAbsolutePath().toString());
    }

    @Test
    void testCreateGoogleWalletJwt_Success() {
        String token = "valid-token";
        User user = new User();
        user.id = 1L;
        user.setFirstname("John");
        user.setLastname("Doe");

        EventLocation location = new EventLocation();
        location.setName("Test Location");

        Event event = new Event();
        event.id = 1L;
        event.setName("Test Event");
        event.setStartTime(Instant.now().plusSeconds(3600));
        event.setEventLocation(location);

        Seat seat = new Seat();
        seat.setSeatNumber("A1");

        Reservation reservation = new Reservation();
        reservation.setEvent(event);
        reservation.setSeat(seat);

        user.setReservations(Set.of(reservation));

        EmailSeatMapToken seatMapToken = mock(EmailSeatMapToken.class);
        when(seatMapToken.getEvent()).thenReturn(event);
        when(seatMapToken.getUser()).thenReturn(user);
        when(seatMapToken.getExpirationTime()).thenReturn(Instant.now().plusSeconds(3600));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(seatMapToken));

        String jwt = googleWalletService.createGoogleWalletJwt(token);
        assertNotNull(jwt);
        // We could further verify the JWT content if needed
    }

    @Test
    void testCreateGoogleWalletJwt_InvalidToken() {
        when(tokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    googleWalletService.createGoogleWalletJwt("invalid");
                });
    }

    @Test
    void testCreateGoogleWalletJwt_ExpiredToken() {
        EmailSeatMapToken seatMapToken = mock(EmailSeatMapToken.class);
        when(seatMapToken.getExpirationTime()).thenReturn(Instant.now().minusSeconds(3600));
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(seatMapToken));

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    googleWalletService.createGoogleWalletJwt("expired");
                });
    }
}
