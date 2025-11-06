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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * EmailSeatMapToken entity represents a token that allows access to a seat map image for a specific
 * user and event. This token is included in email notifications and enables users to view their
 * seat reservations via a secure link.
 *
 * <p>Multiple tokens can be generated for the same user-event combination (e.g., for different
 * emails). Expired tokens are automatically cleaned up by a scheduled task.
 */
@Entity
@Table(
        name = "email_seat_map_tokens",
        indexes = {
            @Index(name = "idx_token", columnList = "token"),
            @Index(name = "idx_expiration_time", columnList = "expiration_time"),
            @Index(name = "idx_user_event", columnList = "user_id, event_id")
        })
public class EmailSeatMapToken extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(unique = true, nullable = false, length = 64)
    private String token;

    @Column(name = "expiration_time", nullable = false)
    private Instant expirationTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "email_seat_map_token_seats",
            joinColumns = @JoinColumn(name = "token_id"))
    @Column(name = "seat_number")
    private Set<String> newReservedSeatNumbers = new HashSet<>();

    /** Constructor for JPA. */
    public EmailSeatMapToken() {}

    /**
     * Creates a new EmailSeatMapToken.
     *
     * @param user the user for whom the token is created
     * @param event the event associated with the token
     * @param token the unique token string
     * @param expirationTime the expiration time of the token
     * @param createdAt the creation time of the token
     * @param newReservedSeatNumbers the set of newly reserved seat numbers
     */
    public EmailSeatMapToken(
            User user,
            Event event,
            String token,
            Instant expirationTime,
            Instant createdAt,
            Set<String> newReservedSeatNumbers) {
        this.user = user;
        this.event = event;
        this.token = token;
        this.expirationTime = expirationTime;
        this.createdAt = createdAt;
        this.newReservedSeatNumbers =
                newReservedSeatNumbers != null
                        ? new HashSet<>(newReservedSeatNumbers)
                        : new HashSet<>();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Instant expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<String> getNewReservedSeatNumbers() {
        return newReservedSeatNumbers;
    }

    public void setNewReservedSeatNumbers(Set<String> newReservedSeatNumbers) {
        this.newReservedSeatNumbers =
                newReservedSeatNumbers != null ? newReservedSeatNumbers : new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailSeatMapToken that = (EmailSeatMapToken) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(user, that.user)
                && Objects.equals(event, that.event)
                && Objects.equals(token, that.token)
                && Objects.equals(expirationTime, that.expirationTime)
                && Objects.equals(newReservedSeatNumbers, that.newReservedSeatNumbers);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(user, event, token, expirationTime, newReservedSeatNumbers);
    }

    @Override
    public String toString() {
        return "EmailSeatMapToken{"
                + "id="
                + id
                + ", user="
                + (user != null ? user.id : null)
                + ", event="
                + (event != null ? event.id : null)
                + ", token='"
                + token
                + '\''
                + ", expirationTime="
                + expirationTime
                + ", createdAt="
                + createdAt
                + ", newReservedSeatNumbers="
                + newReservedSeatNumbers
                + '}';
    }
}
