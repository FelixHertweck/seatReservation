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

package de.felixhertweck.seatreservation.eventManagement.dto;

import java.time.LocalDateTime;

import de.felixhertweck.seatreservation.model.entity.Reservation;

public class ReservationExportDTO {
    private Long id;
    private String seatNumber;
    private String firstName;
    private String lastName;
    private LocalDateTime reservationDate;

    public ReservationExportDTO(
            Long id,
            String seatNumber,
            String firstName,
            String lastName,
            LocalDateTime reservationDate) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.reservationDate = reservationDate;
    }

    public static ReservationExportDTO toDTO(Reservation reservation, Long exportId) {
        return new ReservationExportDTO(
                exportId,
                reservation.getSeat().getSeatNumber(),
                reservation.getUser().getFirstname(),
                reservation.getUser().getLastname(),
                reservation.getReservationDate());
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

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDateTime reservationDate) {
        this.reservationDate = reservationDate;
    }
}
