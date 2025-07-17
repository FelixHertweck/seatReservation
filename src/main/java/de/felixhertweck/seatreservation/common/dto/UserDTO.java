package de.felixhertweck.seatreservation.common.dto;

import de.felixhertweck.seatreservation.model.entity.User;

public record UserDTO(Long id, String username, String firstName, String lastName, String email) {

    public UserDTO(User user) {
        this(
                user.getId(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail());
    }
}
