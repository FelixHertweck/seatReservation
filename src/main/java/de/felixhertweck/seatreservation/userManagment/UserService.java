package de.felixhertweck.seatreservation.userManagment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.entity.User;
import de.felixhertweck.seatreservation.repository.UserRepository;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.DuplicateUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.InvalidUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;
import io.quarkus.elytron.security.common.BcryptUtil;

@ApplicationScoped
public class UserService {

    @Inject UserRepository userRepository;

    @Transactional
    public User createUser(UserCreationDTO userCreationDTO) {
        if (userCreationDTO == null
                || userCreationDTO.getUsername() == null
                || userCreationDTO.getUsername().trim().isEmpty()
                || userCreationDTO.getEmail() == null
                || userCreationDTO.getEmail().trim().isEmpty()
                || userCreationDTO.getPassword() == null
                || userCreationDTO.getPassword().trim().isEmpty()) {
            throw new InvalidUserException("Username, email, and password cannot be empty.");
        }
        if (userRepository.findByUsername(userCreationDTO.getUsername()) != null) {
            throw new DuplicateUserException(
                    "User with username " + userCreationDTO.getUsername() + " already exists.");
        }
        if (userRepository.find("email", userCreationDTO.getEmail()).firstResult() != null) {
            throw new DuplicateUserException(
                    "User with email " + userCreationDTO.getEmail() + " already exists.");
        }

        User user = new User();
        user.setUsername(userCreationDTO.getUsername());
        user.setEmail(userCreationDTO.getEmail());
        user.setPasswordHash(
                BcryptUtil.bcryptHash(userCreationDTO.getPassword())); // Hash the password
        user.setFirstname(userCreationDTO.getFirstname());
        user.setLastname(userCreationDTO.getLastname());
        user.setRoles(new HashSet<>(List.of(Roles.USER))); // Default role for new users

        userRepository.persist(user);
        return user;
    }

    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = userRepository.findById(id);
        if (existingUser == null) {
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
        if (user == null) {
            throw new InvalidUserException("User data cannot be null.");
        }

        // Check for duplicate username if changed
        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            if (userRepository.findByUsername(user.getUsername()) != null) {
                throw new DuplicateUserException(
                        "User with username " + user.getUsername() + " already exists.");
            }
            existingUser.setUsername(user.getUsername());
        }

        // Check for duplicate email if changed
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.find("email", user.getEmail()).firstResult() != null) {
                throw new DuplicateUserException(
                        "User with email " + user.getEmail() + " already exists.");
            }
            existingUser.setEmail(user.getEmail());
        }

        if (user.getFirstname() != null) {
            existingUser.setFirstname(user.getFirstname());
        }
        if (user.getLastname() != null) {
            existingUser.setLastname(user.getLastname());
        }
        if (user.getPasswordHash() != null && !user.getPasswordHash().trim().isEmpty()) {
            existingUser.setPasswordHash(
                    BcryptUtil.bcryptHash(user.getPasswordHash())); // Hash the password
        }
        if (user.getRoles() != null) {
            existingUser.setRoles(user.getRoles());
        }
        userRepository.persist(existingUser); // Panache persist handles update if entity is managed
        return existingUser;
    }

    @Transactional
    public void deleteUser(Long id) {
        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
    }

    public User getUserById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
        return user;
    }

    public List<User> getAllUsers() {
        return userRepository.listAll();
    }

    public List<String> getAvailableRoles() {
        return Arrays.asList(Roles.ALL_ROLES);
    }

    @Transactional
    public User updateUserProfile(String username, UserProfileUpdateDTO userProfileUpdateDTO) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser == null) {
            throw new UserNotFoundException("User with username " + username + " not found.");
        }

        if (userProfileUpdateDTO == null) {
            throw new InvalidUserException("User profile data cannot be null.");
        }

        // Update email if provided and different
        if (userProfileUpdateDTO.getEmail() != null
                && !userProfileUpdateDTO.getEmail().trim().isEmpty()
                && !userProfileUpdateDTO.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.find("email", userProfileUpdateDTO.getEmail()).firstResult()
                    != null) {
                throw new DuplicateUserException(
                        "User with email " + userProfileUpdateDTO.getEmail() + " already exists.");
            }
            existingUser.setEmail(userProfileUpdateDTO.getEmail());
        }

        // Update firstname if provided
        if (userProfileUpdateDTO.getFirstname() != null
                && !userProfileUpdateDTO.getFirstname().trim().isEmpty()) {
            existingUser.setFirstname(userProfileUpdateDTO.getFirstname());
        }

        // Update lastname if provided
        if (userProfileUpdateDTO.getLastname() != null
                && !userProfileUpdateDTO.getLastname().trim().isEmpty()) {
            existingUser.setLastname(userProfileUpdateDTO.getLastname());
        }

        // Update password if provided (in a real app, hash this!)
        if (userProfileUpdateDTO.getPassword() != null
                && !userProfileUpdateDTO.getPassword().trim().isEmpty()) {
            existingUser.setPasswordHash(
                    BcryptUtil.bcryptHash(userProfileUpdateDTO.getPassword())); // Hash the password
        }

        userRepository.persist(existingUser);
        return existingUser;
    }
}
