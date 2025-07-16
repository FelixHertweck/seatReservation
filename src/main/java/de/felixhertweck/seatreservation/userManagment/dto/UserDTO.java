package de.felixhertweck.seatreservation.userManagment.dto;

import de.felixhertweck.seatreservation.model.entity.User;

public record UserDTO(Long id, String username) {

    public UserDTO(User user) {
        this(user.getId(), user.getUsername());
    }
}
