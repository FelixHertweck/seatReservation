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
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class LoginAttemptRepositoryTest {

    @Inject LoginAttemptRepository loginAttemptRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        loginAttemptRepository.deleteAll();
    }

    @Test
    void testRecordSuccessfulAttempt() {
        String username = "testuser";

        loginAttemptRepository.recordAttempt(username, true);

        long count = loginAttemptRepository.count("username = ?1 and successful = true", username);
        assertEquals(1L, count);
    }

    @Test
    void testRecordFailedAttempt() {
        String username = "testuser";

        loginAttemptRepository.recordAttempt(username, false);

        long count = loginAttemptRepository.count("username = ?1 and successful = false", username);
        assertEquals(1L, count);
    }

    @Test
    void testCountFailedAttemptsWithinWindow() {
        String username = "testuser";
        Instant now = Instant.now();
        Instant fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES);

        loginAttemptRepository.recordAttempt(username, false);
        loginAttemptRepository.recordAttempt(username, false);
        loginAttemptRepository.recordAttempt(username, false);

        long count = loginAttemptRepository.countFailedAttempts(username, fiveMinutesAgo);
        assertEquals(3L, count);
    }

    @Test
    void testCountFailedAttemptsExcludesSuccessful() {
        String username = "testuser";
        Instant now = Instant.now();
        Instant fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES);

        loginAttemptRepository.recordAttempt(username, false);
        loginAttemptRepository.recordAttempt(username, true);
        loginAttemptRepository.recordAttempt(username, false);

        long count = loginAttemptRepository.countFailedAttempts(username, fiveMinutesAgo);
        assertEquals(2L, count);
    }

    @Test
    void testCountFailedAttemptsExcludesOldAttempts() {
        String username = "testuser";

        loginAttemptRepository.recordAttempt(username, false);

        // Query with a time in the future - should exclude the just-recorded attempt
        Instant futureTime = Instant.now().plus(1, ChronoUnit.SECONDS);
        long count = loginAttemptRepository.countFailedAttempts(username, futureTime);
        assertEquals(0L, count);
    }

    @Test
    void testCountFailedAttemptsForDifferentUsers() {
        String username1 = "testuser1";
        String username2 = "testuser2";
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

        loginAttemptRepository.recordAttempt(username1, false);
        loginAttemptRepository.recordAttempt(username1, false);
        loginAttemptRepository.recordAttempt(username2, false);

        long count1 = loginAttemptRepository.countFailedAttempts(username1, fiveMinutesAgo);
        long count2 = loginAttemptRepository.countFailedAttempts(username2, fiveMinutesAgo);

        assertEquals(2L, count1);
        assertEquals(1L, count2);
    }

    @Test
    @Transactional
    void testDeleteOldAttempts() {
        String username = "testuser";

        loginAttemptRepository.recordAttempt(username, false);
        loginAttemptRepository.recordAttempt(username, false);

        Instant futureTime = Instant.now().plus(1, ChronoUnit.HOURS);
        long deletedCount = loginAttemptRepository.deleteOldAttempts(futureTime);

        assertTrue(deletedCount >= 2L);
        assertEquals(0L, loginAttemptRepository.count());
    }

    @Test
    @Transactional
    void testDeleteOldAttemptsDoesNotDeleteRecent() {
        String username = "testuser";

        loginAttemptRepository.recordAttempt(username, false);

        Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
        long deletedCount = loginAttemptRepository.deleteOldAttempts(pastTime);

        assertEquals(0L, deletedCount);
        assertEquals(1L, loginAttemptRepository.count());
    }

    @Test
    void testGetOldestFailedAttemptTime() {
        String username = "testuser";
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

        loginAttemptRepository.recordAttempt(username, false);
        loginAttemptRepository.recordAttempt(username, false);
        loginAttemptRepository.recordAttempt(username, false);

        Instant oldestAttempt =
                loginAttemptRepository.getOldestFailedAttemptTime(username, fiveMinutesAgo);

        assertNotNull(oldestAttempt);
        assertTrue(oldestAttempt.isAfter(fiveMinutesAgo) || oldestAttempt.equals(fiveMinutesAgo));
    }

    @Test
    void testGetOldestFailedAttemptTime_NoAttempts() {
        String username = "testuser";
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

        Instant oldestAttempt =
                loginAttemptRepository.getOldestFailedAttemptTime(username, fiveMinutesAgo);

        assertNull(oldestAttempt);
    }

    @Test
    void testGetOldestFailedAttemptTime_ExcludesSuccessful() {
        String username = "testuser";
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

        loginAttemptRepository.recordAttempt(username, true);
        loginAttemptRepository.recordAttempt(username, false);

        Instant oldestAttempt =
                loginAttemptRepository.getOldestFailedAttemptTime(username, fiveMinutesAgo);

        assertNotNull(oldestAttempt);
        // The oldest failed attempt should be more recent than a successful one if recorded after
    }
}
