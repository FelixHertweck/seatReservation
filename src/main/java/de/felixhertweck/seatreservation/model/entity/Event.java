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
import java.util.*;
import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "events")
public class Event extends PanacheEntity {

    private String name;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private Instant bookingDeadline;
    private Instant bookingStartTime;

    @ManyToOne(fetch = FetchType.LAZY)
    private EventLocation event_location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventUserAllowance> userAllowances = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private User manager;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations = new ArrayList<>();

    public Event() {}

    public Long getId() {
        return id;
    }

    public Event(
            String name,
            String description,
            Instant startTime,
            Instant endTime,
            Instant bookingDeadline,
            Instant bookingStartTime,
            EventLocation location,
            User manager) {
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bookingDeadline = bookingDeadline;
        this.bookingStartTime = bookingStartTime;
        this.event_location = location;
        this.manager = manager;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getBookingDeadline() {
        return bookingDeadline;
    }

    public void setBookingDeadline(Instant bookingDeadline) {
        this.bookingDeadline = bookingDeadline;
    }

    public Instant getBookingStartTime() {
        return bookingStartTime;
    }

    public void setBookingStartTime(Instant bookingStartTime) {
        this.bookingStartTime = bookingStartTime;
    }

    public EventLocation getEventLocation() {
        return event_location;
    }

    public void setEventLocation(EventLocation event_location) {
        this.event_location = event_location;
    }

    public Set<EventUserAllowance> getUserAllowances() {
        return userAllowances;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        if (id != null && event.id != null) {
            return Objects.equals(id, event.id);
        }
        return Objects.equals(name, event.name)
                && Objects.equals(description, event.description)
                && Objects.equals(startTime, event.startTime)
                && Objects.equals(endTime, event.endTime)
                && Objects.equals(bookingDeadline, event.bookingDeadline)
                && Objects.equals(bookingStartTime, event.bookingStartTime)
                && Objects.equals(event_location, event.event_location)
                && Objects.equals(userAllowances, event.userAllowances)
                && Objects.equals(manager, event.manager)
                && Objects.equals(reservations, event.reservations);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(
                name,
                description,
                startTime,
                endTime,
                bookingDeadline,
                bookingStartTime,
                event_location,
                userAllowances,
                manager,
                reservations);
    }

    @Override
    public String toString() {
        return "Event{"
                + "name='"
                + name
                + '\''
                + ", description='"
                + description
                + '\''
                + ", startTime="
                + startTime
                + ", endTime="
                + endTime
                + ", bookingDeadline="
                + bookingDeadline
                + ", bookingStartTime="
                + bookingStartTime
                + ", event_location="
                + event_location
                + ", userAllowances="
                + userAllowances
                + ", manager="
                + manager
                + ", reservations="
                + reservations
                + '}';
    }
}
