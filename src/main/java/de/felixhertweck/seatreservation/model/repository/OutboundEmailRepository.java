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
import io.quarkus.panache.common.Sort;

/**
 * Repository for the transactional email outbox ({@link OutboundEmail}). Provides the queries used
 * by the dispatcher to pick up due messages and by the cleanup job to purge finished ones.
 */
@ApplicationScoped
public class OutboundEmailRepository implements PanacheRepository<OutboundEmail> {

    /**
     * Finds due messages ready to be sent, oldest first. A message is due when it is still {@link
     * EmailStatus#PENDING} and its next attempt time is not in the future.
     *
     * @param now the reference point in time
     * @param maxResults the maximum number of messages to return
     * @return the due messages, ordered by their scheduled attempt time
     */
    public List<OutboundEmail> findDue(Instant now, int maxResults) {
        return find(
                        "status = ?1 and nextAttemptAt <= ?2",
                        Sort.by("nextAttemptAt").ascending(),
                        EmailStatus.PENDING,
                        now)
                .page(Page.ofSize(maxResults))
                .list();
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

    /**
     * Deletes terminal messages (sent or permanently failed) that were last updated before the
     * given cutoff.
     *
     * <p>Messages are removed entity-by-entity (rather than via a bulk delete) so Hibernate
     * cascades the removal to each message's attachments and recipient collection tables, avoiding
     * orphaned rows and foreign-key violations.
     *
     * @param cutoff messages updated before this instant are removed
     * @return the number of deleted rows
     */
    public long deleteFinishedBefore(Instant cutoff) {
        List<OutboundEmail> finished =
                list(
                        "status in ?1 and updatedAt < ?2",
                        List.of(EmailStatus.SENT, EmailStatus.FAILED),
                        cutoff);
        finished.forEach(this::delete);
        return finished.size();
    }
}
