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
package de.felixhertweck.seatreservation.model.entity;

import java.time.Instant;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** EmailCooldown entity tracking email resend rate limits. */
@Entity
@Table(
        name = "email_cooldowns",
        uniqueConstraints = @UniqueConstraint(columnNames = {"purpose", "cooldown_key"}))
public class EmailCooldown extends AbstractEntity {

    @Column(nullable = false)
    private String purpose;

    @Column(name = "cooldown_key", nullable = false)
    private String cooldownKey;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    /** Default constructor for JPA. */
    public EmailCooldown() {}

    public EmailCooldown(String purpose, String cooldownKey, Instant lastSentAt) {
        this.purpose = purpose;
        this.cooldownKey = cooldownKey;
        this.lastSentAt = lastSentAt;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getCooldownKey() {
        return cooldownKey;
    }

    public void setCooldownKey(String cooldownKey) {
        this.cooldownKey = cooldownKey;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(Instant lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailCooldown that = (EmailCooldown) o;
        return Objects.equals(purpose, that.purpose)
                && Objects.equals(cooldownKey, that.cooldownKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(purpose, cooldownKey);
    }
}
