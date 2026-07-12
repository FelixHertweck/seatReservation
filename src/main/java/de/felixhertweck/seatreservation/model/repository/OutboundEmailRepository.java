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
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EmailStatus;
import de.felixhertweck.seatreservation.model.entity.OutboundEmail;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;

/**
 * Repository for the transactional email outbox ({@link OutboundEmail}). Provides the queries used
 * by the dispatcher to pick up due messages and by the cleanup job to purge finished ones.
 */
@ApplicationScoped
public class OutboundEmailRepository implements PanacheRepository<OutboundEmail> {

    /**
     * Atomically claims up to {@code limit} due messages by flipping them from {@link
     * EmailStatus#PENDING} to {@link EmailStatus#SENDING} in a single {@code UPDATE ... FOR UPDATE
     * SKIP LOCKED} statement.
     *
     * <p>Unlike a separate select-then-update, this holds row locks for the claimed rows only for
     * the duration of the statement itself, so two dispatchers draining concurrently (whether
     * threads in the same instance or separate clustered instances) cannot both claim the same
     * message: the second claimer's subquery skips rows already locked by the first.
     *
     * @param now the reference point in time
     * @param limit the maximum number of messages to claim
     * @return the ids of the claimed messages, oldest scheduled attempt first
     */
    @SuppressWarnings("unchecked")
    public List<Long> claimDue(Instant now, int limit) {
        List<Number> ids =
                getEntityManager()
                        .createNativeQuery(
                                "UPDATE outbound_emails SET status = 'SENDING', updated_at = ?1 "
                                        + "WHERE id IN ("
                                        + "  SELECT id FROM outbound_emails"
                                        + "  WHERE status = 'PENDING' AND next_attempt_at <= ?1"
                                        + "  ORDER BY next_attempt_at ASC"
                                        + "  LIMIT ?2"
                                        + "  FOR UPDATE SKIP LOCKED"
                                        + ") "
                                        + "RETURNING id")
                        .setParameter(1, now)
                        .setParameter(2, limit)
                        .getResultList();
        return ids.stream().map(Number::longValue).toList();
    }

    /**
     * Resets messages that got stuck in {@link EmailStatus#SENDING} (for example because the
     * application crashed mid-send) back to {@link EmailStatus#PENDING} so they can be retried.
     *
     * @param cutoff messages left in {@code SENDING} and last touched before this instant are reset
     * @return the number of reset messages
     */
    public long requeueStaleSending(Instant cutoff) {
        return update(
                "status = ?1, updatedAt = ?2 where status = ?3 and updatedAt < ?4",
                EmailStatus.PENDING,
                Instant.now(),
                EmailStatus.SENDING,
                cutoff);
    }

    /**
     * Counts messages currently in the given status.
     *
     * @param status the status to count
     * @return the number of matching messages
     */
    public long countByStatus(EmailStatus status) {
        return count("status", status);
    }

    /** Number of messages removed per round-trip by {@link #deleteFinishedBefore(Instant)}. */
    private static final int DELETE_BATCH_SIZE = 500;

    /**
     * Deletes terminal messages (sent or permanently failed) that were last updated before the
     * given cutoff.
     *
     * <p>Messages are removed entity-by-entity (rather than via a bulk delete) so Hibernate
     * cascades the removal to each message's attachments and recipient collection tables, avoiding
     * orphaned rows and foreign-key violations. They are fetched and deleted in bounded batches
     * rather than all at once, so a large backlog (e.g. after a prolonged SMTP outage) doesn't pull
     * the entire set into memory in one go. The persistence context is flushed and cleared after
     * each batch so removed (and previously loaded) entities don't keep accumulating in it for the
     * duration of the whole cleanup run.
     *
     * @param cutoff messages updated before this instant are removed
     * @return the number of deleted rows
     */
    public long deleteFinishedBefore(Instant cutoff) {
        long totalDeleted = 0;
        List<OutboundEmail> batch;
        do {
            batch =
                    find(
                                    "status in ?1 and updatedAt < ?2",
                                    List.of(EmailStatus.SENT, EmailStatus.FAILED),
                                    cutoff)
                            .page(Page.ofSize(DELETE_BATCH_SIZE))
                            .list();
            batch.forEach(this::delete);
            getEntityManager().flush();
            getEntityManager().clear();
            totalDeleted += batch.size();
        } while (batch.size() == DELETE_BATCH_SIZE);
        return totalDeleted;
    }
}
