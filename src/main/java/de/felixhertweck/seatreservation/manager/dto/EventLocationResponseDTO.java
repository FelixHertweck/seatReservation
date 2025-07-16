package de.felixhertweck.seatreservation.manager.dto;

import java.util.List;

import de.felixhertweck.seatreservation.model.entity.EventLocation;

public record EventLocationResponseDTO(
        Long id,
        String name,
        String address,
        Integer capacity,
        UserDTO manager,
        List<SeatDTO> seats) {
    public EventLocationResponseDTO(EventLocation eventLocation) {
        this(
                eventLocation.getId(),
                eventLocation.getName(),
                eventLocation.getAddress(),
                eventLocation.getCapacity(),
                eventLocation.getManager() != null ? new UserDTO(eventLocation.getManager()) : null,
                eventLocation.getSeats() != null
                        ? eventLocation.getSeats().stream().map(SeatDTO::new).toList()
                        : List.of());
    }
}
