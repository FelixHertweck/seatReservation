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

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(
        uniqueConstraints = {@UniqueConstraint(columnNames = {"seatNumber", "location_id"})},
        name = "seats")
public class Seat extends PanacheEntity {

    @Column(nullable = false)
    private String seatNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    private EventLocation location;

    @Embedded private Coordinate coordinate = new Coordinate();

    private String seatRow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrance_id")
    private EventLocationEntrance entrance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private EventLocationArea area;

    // Protected: JPA needs a no-arg constructor, but a Seat must never lack a coordinate.
    protected Seat() {}

    public Seat(String seatNumber, String seatRow, EventLocation location) {
        this.seatNumber = seatNumber;
        this.location = location;
        this.seatRow = seatRow;
    }

    public Seat(
            String seatNumber,
            EventLocation location,
            String seatRow,
            int xCoordinate,
            int yCoordinate,
            EventLocationEntrance entrance,
            EventLocationArea area) {
        this.seatNumber = seatNumber;
        this.location = location;
        this.seatRow = seatRow;
        this.coordinate = new Coordinate(xCoordinate, yCoordinate);
        this.entrance = entrance;
        this.area = area;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public EventLocation getLocation() {
        return location;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = Objects.requireNonNull(coordinate, "coordinate must not be null");
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setLocation(EventLocation location) {
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public EventLocationEntrance getEntrance() {
        return entrance;
    }

    public void setEntrance(EventLocationEntrance entrance) {
        this.entrance = entrance;
    }

    public EventLocationArea getArea() {
        return area;
    }

    public void setArea(EventLocationArea area) {
        this.area = area;
    }

    /**
     * Two seats are equal only if both are persisted (non-{@code null} {@code id}) and share that
     * id; a persisted seat is never equal to a transient one, even with matching fields. Only two
     * transient seats (both {@code id == null}) fall back to field-based comparison, so they can be
     * compared/deduplicated before being persisted.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat that = (Seat) o;
        if (id != null || that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(coordinate, that.coordinate)
                && Objects.equals(seatNumber, that.seatNumber)
                && Objects.equals(location, that.location)
                && Objects.equals(seatRow, that.seatRow)
                && Objects.equals(entrance, that.entrance)
                && Objects.equals(area, that.area);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(seatNumber, location, coordinate, seatRow, entrance, area);
    }

    @Override
    public String toString() {
        return "Seat{"
                + "seatNumber='"
                + seatNumber
                + '\''
                + ", location="
                + location
                + ", coordinate="
                + coordinate
                + ", seatRow='"
                + seatRow
                + '\''
                + ", entrance='"
                + entrance
                + '\''
                + ", area='"
                + area
                + '\''
                + ", id="
                + id
                + '}';
    }
}
