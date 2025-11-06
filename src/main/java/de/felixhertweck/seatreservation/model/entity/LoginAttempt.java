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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * Entity to track login attempts for rate limiting purposes. Stores information about failed login
 * attempts to prevent brute force attacks.
 */
@Entity
@Table(name = "login_attempts")
public class LoginAttempt extends PanacheEntity {

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Instant attemptTime;

    @Column(nullable = false)
    private Boolean successful;

    /** Constructor for JPA. */
    public LoginAttempt() {}

    /**
     * Creates a new login attempt record.
     *
     * @param username the username of the login attempt
     * @param attemptTime the time of the login attempt
     * @param successful whether the login attempt was successful
     */
    public LoginAttempt(String username, Instant attemptTime, Boolean successful) {
        this.username = username;
        this.attemptTime = attemptTime;
        this.successful = successful;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
