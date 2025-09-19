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
import jakarta.persistence.*;

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

    private int xCoordinate;

    private int yCoordinate;

    private String seatRow;

    public Seat() {}

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
            int yCoordinate) {
        this.seatNumber = seatNumber;
        this.location = location;
        this.seatRow = seatRow;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public EventLocation getLocation() {
        return location;
    }

    public int getxCoordinate() {
        return xCoordinate;
    }

    public int getyCoordinate() {
        return yCoordinate;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setLocation(EventLocation location) {
        this.location = location;
    }

    public void setxCoordinate(int xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public void setyCoordinate(int yCoordinate) {
        this.yCoordinate = yCoordinate;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat that = (Seat) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return xCoordinate == that.xCoordinate
                && yCoordinate == that.yCoordinate
                && Objects.equals(seatNumber, that.seatNumber)
                && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(seatNumber, location, xCoordinate, yCoordinate);
    }

    @Override
    public String toString() {
        return "Seat{"
                + "seatNumber='"
                + seatNumber
                + '\''
                + ", location="
                + location
                + ", xCoordinate="
                + xCoordinate
                + ", yCoordinate="
                + yCoordinate
                + ", id="
                + id
                + '}';
    }
}
