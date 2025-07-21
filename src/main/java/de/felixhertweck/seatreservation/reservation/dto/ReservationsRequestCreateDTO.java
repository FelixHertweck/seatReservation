package de.felixhertweck.seatreservation.reservation.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class ReservationsRequestCreateDTO {
    @NotNull(message = "Event ID must not be null")
    private Long eventId;

    @NotNull(message = "Seat IDs must not be null")
    @NotEmpty(message = "Seat IDs must not be empty")
    private List<Long> seatIds;

    public ReservationsRequestCreateDTO() {}

    public ReservationsRequestCreateDTO(Long eventId, List<Long> seatIds) {
        this.eventId = eventId;
        this.seatIds = seatIds;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }
}
