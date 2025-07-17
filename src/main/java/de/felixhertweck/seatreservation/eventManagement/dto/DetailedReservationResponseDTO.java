package de.felixhertweck.seatreservation.eventManagement.dto;

import java.time.LocalDateTime;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.model.entity.Reservation;

public record DetailedReservationResponseDTO(
        Long id,
        UserDTO user,
        DetailedEventResponseDTO event,
        SeatDTO seat,
        LocalDateTime reservationDateTime) {
    public DetailedReservationResponseDTO(Reservation reservation) {
        this(
                reservation.id,
                new UserDTO(reservation.getUser()),
                new DetailedEventResponseDTO(reservation.getEvent()),
                new SeatDTO(reservation.getSeat()),
                reservation.getReservationDate());
    }
}
