package de.felixhertweck.seatreservation.eventManagement.dto;

import java.util.List;

public class BlockSeatsRequestDTO {

    private Long eventId;
    private List<Long> seatIds;

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
