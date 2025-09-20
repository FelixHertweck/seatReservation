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
import java.util.List;
import java.util.Objects;
import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "eventlocations")
public class EventLocation extends PanacheEntity {

    private String name;
    private String address;
    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    private User manager;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(
            mappedBy = "eventLocation",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<EventLocationMarker> markers = new ArrayList<>();

    public EventLocation() {}

    public EventLocation(String name, String address, User manager, Integer capacity) {
        this.name = name;
        this.address = address;
        this.manager = manager;
        this.capacity = capacity;
    }

    public EventLocation(
            String name,
            String address,
            User manager,
            Integer capacity,
            List<EventLocationMarker> markers) {
        this.name = name;
        this.address = address;
        this.manager = manager;
        this.capacity = capacity;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public Long getId() {
        return id;
    }

    public List<EventLocationMarker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<EventLocationMarker> markers) {
        this.markers = markers;
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
                && Objects.equals(capacity, that.capacity)
                && Objects.equals(manager, that.manager)
                && Objects.equals(seats, that.seats);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(name, address, capacity, manager, seats);
    }

    @Override
    public String toString() {
        return "EventLocation{"
                + "id="
                + id
                + ", seats="
                + seats
                + ", manager="
                + manager
                + ", capacity="
                + capacity
                + ", address='"
                + address
                + '\''
                + ", name='"
                + name
                + '\''
                + '}';
    }
}
