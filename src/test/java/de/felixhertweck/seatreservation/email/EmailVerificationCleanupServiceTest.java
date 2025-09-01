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

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class EmailVerificationCleanupServiceTest {
    @InjectMock EmailVerificationRepository emailVerificationRepository;

    @Inject EmailVerificationCleanupService emailVerificationCleanupService;

    @BeforeEach
    void setUp() {
        emailVerificationCleanupService.batchSize = 100;
        emailVerificationCleanupService.cleanupEnabled = true;
    }

    @Test
    void performManualCleanup_Success() {
        Mockito.when(emailVerificationRepository.deleteExpiredEntries()).thenReturn(5L);

        long deletedCount = emailVerificationCleanupService.performManualCleanup();

        assertEquals(5L, deletedCount);
        Mockito.verify(emailVerificationRepository, Mockito.times(1)).deleteExpiredEntries();
    }

    @Test
    void performManualCleanup_NoExpiredTokens() {
        Mockito.when(emailVerificationRepository.deleteExpiredEntries()).thenReturn(0L);

        long deletedCount = emailVerificationCleanupService.performManualCleanup();

        assertEquals(0L, deletedCount);
        Mockito.verify(emailVerificationRepository, Mockito.times(1)).deleteExpiredEntries();
    }

    @Test
    void scheduledCleanup_Success() {
        Mockito.when(emailVerificationRepository.deleteExpiredEntriesInBatch(100))
                .thenReturn(100L)
                .thenReturn(50L)
                .thenReturn(0L);

        emailVerificationCleanupService.cleanupExpiredVerifications();

        Mockito.verify(emailVerificationRepository, Mockito.times(2))
                .deleteExpiredEntriesInBatch(100);
    }
}
