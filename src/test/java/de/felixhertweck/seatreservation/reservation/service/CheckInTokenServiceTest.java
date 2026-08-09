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
package de.felixhertweck.seatreservation.reservation.service;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.util.Optional;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.CheckInTokenRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CheckInTokenServiceTest {

    @Inject CheckInTokenService checkInTokenService;

    @InjectMock CheckInTokenRepository checkInTokenRepository;

    private User user;
    private User boxofficeUser;
    private Event event;

    @BeforeEach
    void setup() {
        user = new User();
        user.id = id(1);
        user.setUsername("alice");

        boxofficeUser = new User();
        boxofficeUser.id = id(99);
        boxofficeUser.setUsername("boxoffice");

        event = new Event();
        event.id = id(10);
    }

    @Test
    void testGetOrCreateForUser_returnsExistingTokenWhenFound() {
        CheckInToken existingToken = new CheckInToken(user, event, "EXISTING_TOKEN");
        when(checkInTokenRepository.findByUserAndEvent(user, event))
                .thenReturn(Optional.of(existingToken));

        CheckInToken result = checkInTokenService.getOrCreateForUser(user, event);

        assertNotNull(result);
        assertEquals("EXISTING_TOKEN", result.getToken());
        verify(checkInTokenRepository, times(0)).persist(any(CheckInToken.class));
    }

    @Test
    void testGetOrCreateForUser_createsNewTokenWhenNotFound() {
        when(checkInTokenRepository.findByUserAndEvent(user, event)).thenReturn(Optional.empty());

        CheckInToken result = checkInTokenService.getOrCreateForUser(user, event);

        assertNotNull(result);
        assertNotNull(result.getToken());
        assertEquals(user, result.getUser());
        assertEquals(event, result.getEvent());
        verify(checkInTokenRepository, times(1)).persist(any(CheckInToken.class));
    }

    @Test
    void testCreateFresh_alwaysCreatesNewToken() {
        CheckInToken token1 = checkInTokenService.createFresh(boxofficeUser, event);
        CheckInToken token2 = checkInTokenService.createFresh(boxofficeUser, event);

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1.getToken(), token2.getToken());
        verify(checkInTokenRepository, times(2)).persist(any(CheckInToken.class));
    }
}
