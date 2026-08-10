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
package de.felixhertweck.seatreservation.wallet.service;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the routing, authorization, and reservation-lookup logic inside {@link WalletPassService}.
 *
 * <p>The individual generators ({@link GoogleWalletPassGenerator}, {@link
 * AppleWalletPassGenerator}) require real credential files (SA private key, PKCS#12 certificate) to
 * work end-to-end; their signing logic is covered separately in dedicated unit tests. Here the
 * generators are mocked via {@link io.quarkus.test.InjectMock} so we can test the service layer in
 * isolation without needing those files on the CI filesystem.
 */
@QuarkusTest
@TestProfile(WalletPassServiceTest.EnabledWalletProfile.class)
class WalletPassServiceTest {

    public static class EnabledWalletProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "wallet.google.enabled", "true",
                    "wallet.apple.enabled", "true");
        }
    }

    @Inject WalletPassService walletPassService;

    @InjectMock ReservationRepository reservationRepository;

    // Mock the generators so no real signing/key-file access happens
    @InjectMock GoogleWalletPassGenerator googleWalletPassGenerator;
    @InjectMock AppleWalletPassGenerator appleWalletPassGenerator;
    @InjectMock GenericPkpassGenerator genericPkpassGenerator;

    private User user;
    private User otherUser;
    private Event event;
    private EventLocation location;
    private Seat seat;
    private Reservation reservation;
    private CheckInToken token;

    private static final WalletPassResponseDTO GOOGLE_STUB =
            WalletPassResponseDTO.forGoogle("https://pay.google.com/gp/v/save/stubtoken");
    private static final WalletPassResponseDTO APPLE_STUB =
            WalletPassResponseDTO.forApple(new byte[] {1, 2, 3}, "ticket_stub.pkpass");
    private static final WalletPassResponseDTO GENERIC_STUB =
            WalletPassResponseDTO.forGenericPkpass(new byte[] {1, 2, 3}, "ticket_generic.pkpass");

    @BeforeEach
    void setup() {
        user = new User();
        user.id = id(1);
        user.setUsername("alice");
        user.setEmail("alice@example.com");

        otherUser = new User();
        otherUser.id = id(2);
        otherUser.setUsername("bob");

        location = new EventLocation("Stadthalle", "Hauptstrasse 1", user);
        location.id = id(100);

        event = new Event();
        event.id = id(10);
        event.setName("Konzertevent");
        event.setDescription("Tolles Konzert");
        event.setStartTime(Instant.now());
        event.setEventLocation(location);

        seat = new Seat("4", "1", location);
        seat.id = id(50);

        token = new CheckInToken(user, event, "VALID_TOKEN_123");

        reservation =
                new Reservation(
                        user, event, seat, Instant.now(), ReservationStatus.RESERVED, token);
        reservation.id = id(999);

        when(reservationRepository.findByUserAndEvent(user, event))
                .thenReturn(List.of(reservation));

        // Wire up mocked generators
        when(googleWalletPassGenerator.getProvider()).thenReturn(WalletProvider.GOOGLE);
        when(googleWalletPassGenerator.generatePass(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(GOOGLE_STUB);

        when(appleWalletPassGenerator.getProvider()).thenReturn(WalletProvider.APPLE);
        when(appleWalletPassGenerator.generatePass(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(APPLE_STUB);

        when(genericPkpassGenerator.getProvider()).thenReturn(WalletProvider.GENERIC_PKPASS);
        when(genericPkpassGenerator.generatePass(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(GENERIC_STUB);
    }

    @Test
    void testGeneratePass_GoogleWallet_success() {
        when(reservationRepository.findByIdOptional(id(999))).thenReturn(Optional.of(reservation));

        WalletPassResponseDTO response =
                walletPassService.generatePass(id(999), user, WalletProvider.GOOGLE);

        assertNotNull(response);
        assertEquals(WalletProvider.GOOGLE, response.provider());
        assertNotNull(response.url());
        assertTrue(response.url().startsWith("https://pay.google.com/gp/v/save/"));
    }

    @Test
    void testGeneratePass_AppleWallet_success() {
        when(reservationRepository.findByIdOptional(id(999))).thenReturn(Optional.of(reservation));

        WalletPassResponseDTO response =
                walletPassService.generatePass(id(999), user, WalletProvider.APPLE);

        assertNotNull(response);
        assertEquals(WalletProvider.APPLE, response.provider());
        assertNotNull(response.content());
        assertTrue(response.content().length > 0);
        assertEquals("application/vnd.apple.pkpass", response.contentType());
    }

    @Test
    void testGeneratePass_GenericPkpass_success() {
        when(reservationRepository.findByIdOptional(id(999))).thenReturn(Optional.of(reservation));

        WalletPassResponseDTO response =
                walletPassService.generatePass(id(999), user, WalletProvider.GENERIC_PKPASS);

        assertNotNull(response);
        assertEquals(WalletProvider.GENERIC_PKPASS, response.provider());
        assertNotNull(response.content());
        assertTrue(response.content().length > 0);
        assertEquals("application/vnd.apple.pkpass", response.contentType());
    }

    @Test
    void testGeneratePass_throwsReservationNotFound() {
        when(reservationRepository.findByIdOptional(id(999))).thenReturn(Optional.empty());

        assertThrows(
                ReservationNotFoundException.class,
                () -> walletPassService.generatePass(id(999), user, WalletProvider.GOOGLE));
    }

    @Test
    void testGeneratePass_throwsSecurityException_whenWrongUser() {
        when(reservationRepository.findByIdOptional(id(999))).thenReturn(Optional.of(reservation));

        assertThrows(
                SecurityException.class,
                () -> walletPassService.generatePass(id(999), otherUser, WalletProvider.GOOGLE));
    }
}
