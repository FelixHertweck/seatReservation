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

package de.felixhertweck.seatreservation.management.dto;

import java.time.Instant;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ReservationExportDTO {
    private Long id;
    private String seatNumber;
    private String firstName;
    private String lastName;
    private Instant reservationDate;

    public ReservationExportDTO(Reservation reservation, Long exportId) {
        this.id = exportId;
        this.seatNumber = reservation.getSeat().getSeatNumber();
        this.firstName = reservation.getUser().getFirstname();
        this.lastName = reservation.getUser().getLastname();
        this.reservationDate = reservation.getReservationDate();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Instant getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Instant reservationDate) {
        this.reservationDate = reservationDate;
    }
}
