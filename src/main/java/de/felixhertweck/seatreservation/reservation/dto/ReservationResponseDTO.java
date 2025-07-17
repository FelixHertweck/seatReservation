package de.felixhertweck.seatreservation.reservation.dto;

import java.time.LocalDateTime;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.model.entity.Reservation;

public record ReservationResponseDTO(
        Long id, Long userId, Long eventId, SeatDTO seat, LocalDateTime reservationDateTime) {
    public ReservationResponseDTO(Reservation reservation) {
        this(
                reservation.id,
                reservation.getUser().id,
                reservation.getEvent().id,
                new SeatDTO(reservation.getSeat()),
                reservation.getReservationDate());
    }
}
