package de.felixhertweck.seatreservation.reservation.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class ReservationRequestCreateDTO {
    @NotNull(message = "Event ID must not be null")
    public Long eventId;

    @NotNull(message = "Seat IDs must not be null")
    @NotEmpty(message = "Seat IDs must not be empty")
    public List<Long> seatIds;
}
