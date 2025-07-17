package de.felixhertweck.seatreservation.common.dto;

import java.util.List;

import de.felixhertweck.seatreservation.model.entity.EventLocation;

public record EventLocationResponseDTO(
        Long id,
        String name,
        String address,
        Integer capacity,
        LimitedUserInfoDTO manager,
        List<SeatDTO> seats) {
    public EventLocationResponseDTO(EventLocation eventLocation) {
        this(
                eventLocation.getId(),
                eventLocation.getName(),
                eventLocation.getAddress(),
                eventLocation.getCapacity(),
                eventLocation.getManager() != null
                        ? new LimitedUserInfoDTO(eventLocation.getManager())
                        : null,
                eventLocation.getSeats() != null
                        ? eventLocation.getSeats().stream().map(SeatDTO::new).toList()
                        : List.of());
    }
}
