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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Entity to track 2FA challenge verification attempts for rate limiting purposes. Kept separate
 * from {@link LoginAttempt} (and keyed by user rather than username) so that requesting a fresh
 * challenge does not reset an attacker's failed-attempt budget.
 */
@Entity
@Table(name = "two_factor_attempts")
public class TwoFactorAttempt extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "attempt_time", nullable = false)
    private Instant attemptTime;

    @Column(nullable = false)
    private Boolean successful;

    public TwoFactorAttempt() {}

    public TwoFactorAttempt(User user, Instant attemptTime, Boolean successful) {
        this.user = user;
        this.attemptTime = attemptTime;
        this.successful = successful;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getAttemptTime() {
        return attemptTime;
    }

    public void setAttemptTime(Instant attemptTime) {
        this.attemptTime = attemptTime;
    }

    public Boolean getSuccessful() {
        return successful;
    }

    public void setSuccessful(Boolean successful) {
        this.successful = successful;
    }
}
