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
package de.felixhertweck.seatreservation.security.service;

import java.time.Instant;
import java.util.Optional;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.repository.EmailCooldownRepository;
import de.felixhertweck.seatreservation.security.service.EmailCooldownService.Purpose;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class EmailCooldownServiceTest {

    @InjectMock EmailCooldownRepository emailCooldownRepository;

    @Inject EmailCooldownService emailCooldownService;

    @Test
    void testCheckAndRecordFirstCallAllowed() {
        String key = "user-id-123";
        when(emailCooldownRepository.findLastSentAt(Purpose.EMAIL_CONFIRMATION_RESEND.name(), key))
                .thenReturn(null);

        Optional<Instant> retryAfter =
                emailCooldownService.checkAndRecord(Purpose.EMAIL_CONFIRMATION_RESEND, key);

        assertFalse(retryAfter.isPresent());
        verify(emailCooldownRepository)
                .recordSent(
                        eq(Purpose.EMAIL_CONFIRMATION_RESEND.name()), eq(key), any(Instant.class));
    }

    @Test
    void testCheckAndRecordWithinCooldownWindowBlocked() {
        String key = "user-id-123";
        Instant recent = Instant.now().minusSeconds(10);
        when(emailCooldownRepository.findLastSentAt(Purpose.EMAIL_CONFIRMATION_RESEND.name(), key))
                .thenReturn(recent);

        Optional<Instant> retryAfter =
                emailCooldownService.checkAndRecord(Purpose.EMAIL_CONFIRMATION_RESEND, key);

        assertTrue(retryAfter.isPresent());
        assertTrue(retryAfter.get().isAfter(Instant.now()));
    }

    @Test
    void testCheckAndRecordAfterCooldownWindowAllowed() {
        String key = "user-id-123";
        Instant oldSent = Instant.now().minusSeconds(120);
        when(emailCooldownRepository.findLastSentAt(Purpose.EMAIL_CONFIRMATION_RESEND.name(), key))
                .thenReturn(oldSent);

        Optional<Instant> retryAfter =
                emailCooldownService.checkAndRecord(Purpose.EMAIL_CONFIRMATION_RESEND, key);

        assertFalse(retryAfter.isPresent());
        verify(emailCooldownRepository)
                .recordSent(
                        eq(Purpose.EMAIL_CONFIRMATION_RESEND.name()), eq(key), any(Instant.class));
    }
}
