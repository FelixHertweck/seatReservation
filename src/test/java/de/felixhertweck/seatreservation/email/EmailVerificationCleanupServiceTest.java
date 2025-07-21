package de.felixhertweck.seatreservation.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCleanupServiceTest {

    @Mock Logger LOG;

    @Mock EmailVerificationRepository emailVerificationRepository;

    @InjectMocks EmailVerificationCleanupService emailVerificationCleanupService;

    @BeforeEach
    void setUp() {
        emailVerificationCleanupService.batchSize = 100;
        emailVerificationCleanupService.cleanupEnabled = true;
    }

    @Test
    void performManualCleanup_Success() {
        when(emailVerificationRepository.deleteExpiredEntries()).thenReturn(5L);

        long deletedCount = emailVerificationCleanupService.performManualCleanup();

        assertEquals(5L, deletedCount);
        verify(emailVerificationRepository, times(1)).deleteExpiredEntries();
    }

    @Test
    void performManualCleanup_NoExpiredTokens() {
        when(emailVerificationRepository.deleteExpiredEntries()).thenReturn(0L);

        long deletedCount = emailVerificationCleanupService.performManualCleanup();

        assertEquals(0L, deletedCount);
        verify(emailVerificationRepository, times(1)).deleteExpiredEntries();
    }

    @Test
    void scheduledCleanup_Success() {
        when(emailVerificationRepository.deleteExpiredEntriesInBatch(100))
                .thenReturn(100L)
                .thenReturn(50L)
                .thenReturn(0L);

        emailVerificationCleanupService.cleanupExpiredVerifications();

        verify(emailVerificationRepository, times(2)).deleteExpiredEntriesInBatch(100);
    }

    @Test
    void scheduledCleanup_Disabled() {
        emailVerificationCleanupService.cleanupEnabled = false;

        emailVerificationCleanupService.cleanupExpiredVerifications();

        verify(emailVerificationRepository, never()).deleteExpiredEntriesInBatch(anyInt());
    }
}
