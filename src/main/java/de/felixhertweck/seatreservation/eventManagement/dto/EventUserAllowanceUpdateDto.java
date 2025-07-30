package de.felixhertweck.seatreservation.eventManagement.dto;

import jakarta.validation.constraints.NotNull;

public record EventUserAllowanceUpdateDto(
        @NotNull(message = "Allowance ID must not be null") Long id,
        @NotNull(message = "Event ID must not be null") Long eventId,
        @NotNull(message = "User ID must not be null") Long userId,
        @NotNull(message = "Reservations allowed count must not be null") int reservationsAllowedCount) {}