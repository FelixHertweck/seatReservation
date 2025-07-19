package de.felixhertweck.seatreservation.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class EmailVerificationRepository implements PanacheRepository<EmailVerification> {

    /**
     * Finds all expired email verification entries.
     *
     * @return List of expired EmailVerification entities
     */
    public List<EmailVerification> findExpiredEntries() {
        return find("expirationTime < ?1", LocalDateTime.now()).list();
    }

    /**
     * Finds expired email verification entries with a limit for batch processing.
     *
     * @param batchSize maximum number of entries to return
     * @return List of expired EmailVerification entities (limited by batchSize)
     */
    public List<EmailVerification> findExpiredEntries(int batchSize) {
        return find("expirationTime < ?1", LocalDateTime.now()).page(0, batchSize).list();
    }

    /**
     * Deletes all expired email verification entries.
     *
     * @return number of deleted entries
     */
    @Transactional
    public long deleteExpiredEntries() {
        return delete("expirationTime < ?1", LocalDateTime.now());
    }

    /**
     * Deletes expired email verification entries in batches.
     *
     * @param batchSize maximum number of entries to delete in one batch
     * @return number of deleted entries
     */
    @Transactional
    public long deleteExpiredEntriesInBatch(int batchSize) {
        List<EmailVerification> expiredEntries = findExpiredEntries(batchSize);
        if (expiredEntries.isEmpty()) {
            return 0;
        }

        List<Long> ids = expiredEntries.stream().map(e -> e.id).toList();
        return delete("id in ?1", ids);
    }
}
