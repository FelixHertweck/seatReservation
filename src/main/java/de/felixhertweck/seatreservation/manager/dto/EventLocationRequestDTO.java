package de.felixhertweck.seatreservation.manager.dto;

import jakarta.validation.constraints.NotNull;

public class EventLocationRequestDTO {
    @NotNull(message = "Name must not be null")
    private String name;

    @NotNull(message = "Address must not be null")
    private String address;

    @NotNull(message = "Capacity must not be null")
    private Integer capacity;

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Integer getCapacity() {
        return capacity;
    }
}
