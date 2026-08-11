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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.*;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "eventlocations")
public class EventLocation extends AbstractEntity {

    private String name;
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    // Direct managers, independent of any event. Access via a shared event is computed
    // dynamically (see EventLocationRepository.isUserManager), not duplicated here.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "location_managers",
            joinColumns = @JoinColumn(name = "event_location_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> managers = new HashSet<>();

    // Lazy bags; Hibernate can't join-fetch more than one per query (MultipleBagFetchException),
    // so @BatchSize batches their loads across locations instead.
    private static final int BATCH_SIZE = 32;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = BATCH_SIZE)
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "eventLocation", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = BATCH_SIZE)
    private List<EventLocationMarker> markers = new ArrayList<>();

    @OneToMany(mappedBy = "eventLocation", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = BATCH_SIZE)
    private List<EventLocationArea> areas = new ArrayList<>();

    @OneToMany(mappedBy = "eventLocation", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = BATCH_SIZE)
    private List<EventLocationEntrance> entrances = new ArrayList<>();

    public EventLocation() {}

    public EventLocation(String name, String address, User manager) {
        this.name = name;
        this.address = address;
        this.createdBy = manager;
        this.managers = new HashSet<>();
        if (manager != null) {
            this.managers.add(manager);
        }
    }

    public EventLocation(
            String name, String address, User manager, List<EventLocationMarker> markers) {
        this(name, address, manager);
        this.markers = markers != null ? new ArrayList<>(markers) : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setManager(User manager) {
        this.createdBy = manager;
    }

    public Set<User> getManagers() {
        return managers;
    }

    public void setManagers(Set<User> managers) {
        this.managers = managers;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public List<EventLocationMarker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<EventLocationMarker> markers) {
        this.markers = markers;
    }

    public List<EventLocationArea> getAreas() {
        return areas;
    }

    public void setAreas(List<EventLocationArea> areas) {
        this.areas = areas;
    }

    public List<EventLocationEntrance> getEntrances() {
        return entrances;
    }

    public void setEntrances(List<EventLocationEntrance> entrances) {
        this.entrances = entrances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventLocation that = (EventLocation) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(name, that.name)
                && Objects.equals(address, that.address)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(seats, that.seats);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(name, address, createdBy, seats);
    }

    @Override
    public String toString() {
        return "EventLocation{"
                + "id="
                + id
                + ", seats="
                + seats
                + ", createdBy="
                + createdBy
                + ", address='"
                + address
                + '\''
                + ", name='"
                + name
                + '\''
                + '}';
    }
}
