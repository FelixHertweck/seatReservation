package de.felixhertweck.seatreservation.eventManagement.dto;

import java.time.LocalDateTime;
import java.util.Set;

import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.model.entity.Event;

public record DetailedEventResponseDTO(
        Long id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime bookingDeadline,
        EventLocationResponseDTO location,
        LimitedUserInfoDTO manager,
        Set<EventUserAllowancesDto> eventUserAllowances) {
    public DetailedEventResponseDTO(Event event) {
        this(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getBookingDeadline(),
                new EventLocationResponseDTO(event.getEventLocation()),
                event.getManager() != null ? new LimitedUserInfoDTO(event.getManager()) : null,
                event.getUserAllowances() != null
                        ? event.getUserAllowances().stream()
                                .map(EventUserAllowancesDto::new)
                                .collect(java.util.stream.Collectors.toSet())
                        : Set.of());
    }
}
