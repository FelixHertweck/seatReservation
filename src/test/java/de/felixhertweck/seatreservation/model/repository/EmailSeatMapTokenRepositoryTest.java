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
package de.felixhertweck.seatreservation.model.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.EmailSeatMapToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EmailSeatMapTokenRepositoryTest {

    @Inject EmailSeatMapTokenRepository tokenRepository;
    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")
    void setUp() {
        // Clean up existing tokens
        tokenRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("tokentest" + System.currentTimeMillis());
        testUser.setEmail("tokentest@example.com");
        testUser.setFirstname("Token");
        testUser.setLastname("Test");
        testUser.setPasswordHash("hash");
        testUser.setPasswordSalt("salt");
        testUser.setEmailVerified(true);
        userRepository.persist(testUser);

        // Create test event location
        EventLocation location = new EventLocation();
        location.setName("Token Test Location");
        location.setCapacity(100);
        eventLocationRepository.persist(location);

        // Create test event
        testEvent = new Event();
        testEvent.setName("Token Test Event");
        testEvent.setStartTime(Instant.now().plus(7, ChronoUnit.DAYS));
        testEvent.setEndTime(Instant.now().plus(7, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
        testEvent.setEventLocation(location);
        testEvent.setManager(testUser);
        eventRepository.persist(testEvent);
    }

    @Test
    @Transactional
    void findByToken_Success_WhenTokenExists() {
        String tokenString = UUID.randomUUID().toString();
        EmailSeatMapToken token = new EmailSeatMapToken();
        token.setUser(testUser);
        token.setEvent(testEvent);
        token.setToken(tokenString);
        token.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        token.setCreatedAt(Instant.now());
        token.setNewReservedSeatNumbers(Collections.singleton("A1"));
        tokenRepository.persist(token);

        Optional<EmailSeatMapToken> found = tokenRepository.findByToken(tokenString);

        assertTrue(found.isPresent());
        assertEquals(tokenString, found.get().getToken());
        assertEquals(testUser.id, found.get().getUser().id);
    }

    @Test
    @Transactional
    void findByToken_ReturnsEmpty_WhenTokenNotExists() {
        Optional<EmailSeatMapToken> found = tokenRepository.findByToken("non-existent-token");

        assertFalse(found.isPresent());
    }

    @Test
    @Transactional
    void deleteExpiredTokens_Success() {
        // Create expired token
        EmailSeatMapToken expiredToken = new EmailSeatMapToken();
        expiredToken.setUser(testUser);
        expiredToken.setEvent(testEvent);
        expiredToken.setToken(UUID.randomUUID().toString());
        expiredToken.setExpirationTime(Instant.now().minus(1, ChronoUnit.DAYS));
        expiredToken.setCreatedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        expiredToken.setNewReservedSeatNumbers(Collections.singleton("A1"));
        tokenRepository.persist(expiredToken);

        // Create valid token
        EmailSeatMapToken validToken = new EmailSeatMapToken();
        validToken.setUser(testUser);
        validToken.setEvent(testEvent);
        validToken.setToken(UUID.randomUUID().toString());
        validToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        validToken.setCreatedAt(Instant.now());
        validToken.setNewReservedSeatNumbers(Collections.singleton("B1"));
        tokenRepository.persist(validToken);

        long deletedCount = tokenRepository.deleteExpiredTokens();

        assertEquals(1, deletedCount);
        assertTrue(tokenRepository.findByToken(validToken.getToken()).isPresent());
        assertFalse(tokenRepository.findByToken(expiredToken.getToken()).isPresent());
    }

    @Test
    @Transactional
    void deleteExpiredTokens_ReturnsZero_WhenNoExpiredTokens() {
        // Create only valid token
        EmailSeatMapToken validToken = new EmailSeatMapToken();
        validToken.setUser(testUser);
        validToken.setEvent(testEvent);
        validToken.setToken(UUID.randomUUID().toString());
        validToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        validToken.setCreatedAt(Instant.now());
        validToken.setNewReservedSeatNumbers(Collections.singleton("A1"));
        tokenRepository.persist(validToken);

        long deletedCount = tokenRepository.deleteExpiredTokens();

        assertEquals(0, deletedCount);
        assertTrue(tokenRepository.findByToken(validToken.getToken()).isPresent());
    }

    @Test
    @Transactional
    void deleteByUser_Success() {
        // Create tokens for test user
        EmailSeatMapToken token1 = new EmailSeatMapToken();
        token1.setUser(testUser);
        token1.setEvent(testEvent);
        token1.setToken(UUID.randomUUID().toString());
        token1.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        token1.setCreatedAt(Instant.now());
        token1.setNewReservedSeatNumbers(Collections.singleton("A1"));
        tokenRepository.persist(token1);

        EmailSeatMapToken token2 = new EmailSeatMapToken();
        token2.setUser(testUser);
        token2.setEvent(testEvent);
        token2.setToken(UUID.randomUUID().toString());
        token2.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        token2.setCreatedAt(Instant.now());
        token2.setNewReservedSeatNumbers(Collections.singleton("B1"));
        tokenRepository.persist(token2);

        long deletedCount = tokenRepository.deleteByUser(testUser);

        assertEquals(2, deletedCount);
        assertFalse(tokenRepository.findByToken(token1.getToken()).isPresent());
        assertFalse(tokenRepository.findByToken(token2.getToken()).isPresent());
    }

    @Test
    @Transactional
    void deleteByUser_ReturnsZero_WhenNoTokensExist() {
        long deletedCount = tokenRepository.deleteByUser(testUser);

        assertEquals(0, deletedCount);
    }

    @Test
    @Transactional
    void findExpiredTokens_Success() {
        // Create expired token
        EmailSeatMapToken expiredToken = new EmailSeatMapToken();
        expiredToken.setUser(testUser);
        expiredToken.setEvent(testEvent);
        expiredToken.setToken(UUID.randomUUID().toString());
        expiredToken.setExpirationTime(Instant.now().minus(1, ChronoUnit.DAYS));
        expiredToken.setCreatedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        expiredToken.setNewReservedSeatNumbers(Collections.singleton("A1"));
        tokenRepository.persist(expiredToken);

        // Create valid token
        EmailSeatMapToken validToken = new EmailSeatMapToken();
        validToken.setUser(testUser);
        validToken.setEvent(testEvent);
        validToken.setToken(UUID.randomUUID().toString());
        validToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        validToken.setCreatedAt(Instant.now());
        validToken.setNewReservedSeatNumbers(Collections.singleton("B1"));
        tokenRepository.persist(validToken);

        List<EmailSeatMapToken> expiredTokens = tokenRepository.findExpiredTokens();

        assertEquals(1, expiredTokens.size());
        assertEquals(expiredToken.getToken(), expiredTokens.get(0).getToken());
    }

    @Test
    @Transactional
    void findExpiredTokens_ReturnsEmpty_WhenNoExpiredTokens() {
        // Create only valid token
        EmailSeatMapToken validToken = new EmailSeatMapToken();
        validToken.setUser(testUser);
        validToken.setEvent(testEvent);
        validToken.setToken(UUID.randomUUID().toString());
        validToken.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        validToken.setCreatedAt(Instant.now());
        validToken.setNewReservedSeatNumbers(Collections.singleton("A1"));
        tokenRepository.persist(validToken);

        List<EmailSeatMapToken> expiredTokens = tokenRepository.findExpiredTokens();

        assertTrue(expiredTokens.isEmpty());
    }

    @Test
    @Transactional
    void persist_Success_WithMultipleSeatNumbers() {
        Set<String> seatNumbers = new HashSet<>(Arrays.asList("A1", "A2", "A3"));
        EmailSeatMapToken token = new EmailSeatMapToken();
        token.setUser(testUser);
        token.setEvent(testEvent);
        token.setToken(UUID.randomUUID().toString());
        token.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        token.setCreatedAt(Instant.now());
        token.setNewReservedSeatNumbers(seatNumbers);
        tokenRepository.persist(token);

        Optional<EmailSeatMapToken> found = tokenRepository.findByToken(token.getToken());

        assertTrue(found.isPresent());
        assertEquals(3, found.get().getNewReservedSeatNumbers().size());
        assertTrue(found.get().getNewReservedSeatNumbers().containsAll(seatNumbers));
    }

    @Test
    @Transactional
    void persist_Success_WithEmptySeatNumbers() {
        EmailSeatMapToken token = new EmailSeatMapToken();
        token.setUser(testUser);
        token.setEvent(testEvent);
        token.setToken(UUID.randomUUID().toString());
        token.setExpirationTime(Instant.now().plus(30, ChronoUnit.DAYS));
        token.setCreatedAt(Instant.now());
        token.setNewReservedSeatNumbers(Collections.emptySet());
        tokenRepository.persist(token);

        Optional<EmailSeatMapToken> found = tokenRepository.findByToken(token.getToken());

        assertTrue(found.isPresent());
        assertTrue(found.get().getNewReservedSeatNumbers().isEmpty());
    }
}
