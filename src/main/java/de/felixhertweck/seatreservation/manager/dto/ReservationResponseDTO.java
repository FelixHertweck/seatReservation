package de.felixhertweck.seatreservation.manager.dto;

import java.time.LocalDateTime;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.user.dto.SeatDTO;

public record ReservationResponseDTO(
        Long id,
        UserDTO user,
        EventResponseDTO event,
        SeatDTO seat,
        LocalDateTime reservationDateTime) {
    public ReservationResponseDTO(Reservation reservation) {
        this(
                reservation.id,
                new UserDTO(reservation.getUser()),
                new EventResponseDTO(reservation.getEvent()),
                new SeatDTO(reservation.getSeat()),
                reservation.getReservationDate());
    }
}
