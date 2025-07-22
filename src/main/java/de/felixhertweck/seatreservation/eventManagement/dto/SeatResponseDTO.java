package de.felixhertweck.seatreservation.eventManagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeatResponseDTO(
        Long id,
        String seatNumber,
        EventLocationResponseDTO location,
        int xCoordinate,
        int yCoordinate,
        ReservationStatus status) {
    public SeatResponseDTO(Seat seat) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                new EventLocationResponseDTO(seat.getLocation()),
                seat.getXCoordinate(),
                seat.getYCoordinate(),
                null);
    }

    public SeatResponseDTO(Seat seat, ReservationStatus status) {
        this(
                seat.getId(),
                seat.getSeatNumber(),
                new EventLocationResponseDTO(seat.getLocation()),
                seat.getXCoordinate(),
                seat.getYCoordinate(),
                status);
    }
}
