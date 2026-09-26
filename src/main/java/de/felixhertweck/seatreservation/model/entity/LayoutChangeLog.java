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
package de.felixhertweck.seatreservation.model.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One applied step of a layout batch. Written in the same transaction as the change itself, so the
 * log only ever contains what was actually committed. Ordering within a batch is {@code
 * sequenceNo}. Location and user are plain ids, not foreign keys, so the history outlives both.
 */
@Entity
@Table(
        name = "layout_change_log",
        indexes = {
            @Index(name = "idx_layout_change_log_batch", columnList = "batch_id, sequence_no"),
            @Index(name = "idx_layout_change_log_location", columnList = "event_location_id")
        })
public class LayoutChangeLog extends AbstractEntity {

    @Column(name = "batch_id", nullable = false, columnDefinition = "uuid")
    private UUID batchId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "event_location_id", nullable = false, columnDefinition = "uuid")
    private UUID eventLocationId;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    @Column(nullable = false, length = 16)
    private String action;

    @Column(name = "entity_id", columnDefinition = "uuid")
    private UUID entityId;

    @Column(length = 255)
    private String ref;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Default constructor for JPA. */
    public LayoutChangeLog() {}

    public LayoutChangeLog(
            UUID batchId,
            int sequenceNo,
            UUID eventLocationId,
            UUID userId,
            String entityType,
            String action,
            UUID entityId,
            String ref,
            String payload,
            Instant createdAt) {
        this.batchId = batchId;
        this.sequenceNo = sequenceNo;
        this.eventLocationId = eventLocationId;
        this.userId = userId;
        this.entityType = entityType;
        this.action = action;
        this.entityId = entityId;
        this.ref = ref;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public UUID getEventLocationId() {
        return eventLocationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getAction() {
        return action;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getRef() {
        return ref;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
