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

import java.time.Duration;
import java.time.Instant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.reservation.service.SeatCartAccessGrantStore;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link EventUserAllowanceRepository}'s overridden {@code persist}/{@code delete}
 * invalidate any cached Redis seat-cart access grant for the affected user/event - see {@link
 * SeatCartAccessGrantStore} and the PR #449 review follow-up: without this, a user whose {@code
 * EventUserAllowance} changes (or is revoked entirely) would keep acting on a stale cached grant
 * for up to the grant's TTL.
 */
@QuarkusTest
public class EventUserAllowanceRepositoryTest {

    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject SeatCartAccessGrantStore accessGrantStore;
    @Inject UserRepository userRepository;
    @Inject EventRepository eventRepository;
    @Inject EventLocationRepository eventLocationRepository;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    @Transactional
    void setUp() {
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();

        User manager = userRepository.findByUsernameOptional("manager").orElseThrow();
        testUser = userRepository.findByUsernameOptional("user").orElseThrow();

        EventLocation location = new EventLocation();
        location.setName("Test Location for Allowance Repository Test");
        location.setManager(manager);
        eventLocationRepository.persist(location);

        testEvent = new Event();
        testEvent.setName("Test Event for Allowance Repository Test");
        testEvent.setEventLocation(location);
        testEvent.setStartTime(Instant.now().plusSeconds(Duration.ofDays(2).toSeconds()));
        testEvent.setEndTime(
                Instant.now()
                        .plusSeconds(Duration.ofDays(2).toSeconds())
                        .plusSeconds(Duration.ofHours(2).toSeconds()));
        eventRepository.persist(testEvent);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        eventUserAllowanceRepository.deleteAll();
        eventRepository.deleteAll();
        eventLocationRepository.deleteAll();
    }

    @Test
    @Transactional
    void persist_InvalidatesExistingAccessGrant() {
        accessGrantStore.set(testEvent.id, testUser.id, 2, Duration.ofSeconds(60));
        assertTrue(accessGrantStore.get(testEvent.id, testUser.id).isPresent());

        eventUserAllowanceRepository.persist(new EventUserAllowance(testUser, testEvent, 2));

        assertTrue(accessGrantStore.get(testEvent.id, testUser.id).isEmpty());
    }

    @Test
    @Transactional
    void delete_InvalidatesExistingAccessGrant() {
        EventUserAllowance allowance = new EventUserAllowance(testUser, testEvent, 2);
        eventUserAllowanceRepository.persist(allowance);
        assertNotNull(allowance.id);

        accessGrantStore.set(testEvent.id, testUser.id, 2, Duration.ofSeconds(60));
        assertTrue(accessGrantStore.get(testEvent.id, testUser.id).isPresent());

        eventUserAllowanceRepository.delete(allowance);

        assertTrue(accessGrantStore.get(testEvent.id, testUser.id).isEmpty());
    }
}
