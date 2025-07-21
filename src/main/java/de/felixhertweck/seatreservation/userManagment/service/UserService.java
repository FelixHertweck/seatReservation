package de.felixhertweck.seatreservation.userManagment.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.DuplicateUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.InvalidUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.TokenExpiredException;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;
import io.quarkus.elytron.security.common.BcryptUtil;

@ApplicationScoped
public class UserService {

    @Inject UserRepository userRepository;

    @Inject EmailService emailService;

    @Inject EmailVerificationRepository emailVerificationRepository;

    /**
     * Creates a new user with the provided dto.
     *
     * @param userCreationDTO The DTO containing user creation dto.
     * @return The created UserDTO.
     * @throws InvalidUserException If the provided data is invalid.
     * @throws DuplicateUserException If a user with the same username or email already exists.
     * @throws RuntimeException If an error occurs while sending email confirmation.
     */
    @Transactional
    public UserDTO createUser(UserCreationDTO userCreationDTO)
            throws InvalidUserException, DuplicateUserException {
        if (userCreationDTO == null) {
            throw new InvalidUserException("User creation data cannot be null.");
        }
        if (userCreationDTO.getUsername() == null
                || userCreationDTO.getUsername().trim().isEmpty()) {
            throw new InvalidUserException("Username cannot be empty.");
        }
        if (userCreationDTO.getPassword() == null
                || userCreationDTO.getPassword().trim().isEmpty()) {
            throw new InvalidUserException("Password cannot be empty.");
        }

        if (userRepository.findByUsernameOptional(userCreationDTO.getUsername()).isPresent()) {
            throw new DuplicateUserException(
                    "User with username " + userCreationDTO.getUsername() + " already exists.");
        }
        User user = new User();
        user.setUsername(userCreationDTO.getUsername());
        if (userCreationDTO.getEmail() != null && !userCreationDTO.getEmail().trim().isEmpty()) {
            user.setEmail(userCreationDTO.getEmail());
        }
        user.setPasswordHash(
                BcryptUtil.bcryptHash(userCreationDTO.getPassword())); // Hash the password
        user.setFirstname(userCreationDTO.getFirstname());
        user.setLastname(userCreationDTO.getLastname());
        user.setRoles(new HashSet<>(List.of(Roles.USER))); // Default role for new users

        userRepository.persist(user);

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            try {
                emailService.sendEmailConfirmation(user);
            } catch (IOException e) {
                throw new RuntimeException("Failed to send email confirmation: " + e.getMessage());
            }
        }

        return new UserDTO(user);
    }

    private void updateUserCore(
            User existingUser, String email, String firstname, String lastname, String password) {
        // Update email if provided and different
        if (email != null && !email.trim().isEmpty() && !email.equals(existingUser.getEmail())) {
            existingUser.setEmail(email);
            // Reset email verification status and send confirmation email
            existingUser.setEmailVerified(false);
            try {
                emailService.sendEmailConfirmation(existingUser);
            } catch (IOException e) {
                throw new RuntimeException("Failed to send email confirmation: " + e.getMessage());
            }
        }

        // Update firstname if provided
        if (firstname != null && !firstname.trim().isEmpty()) {
            existingUser.setFirstname(firstname);
        }

        // Update lastname if provided
        if (lastname != null && !lastname.trim().isEmpty()) {
            existingUser.setLastname(lastname);
        }

        // Update password if provided
        if (password != null && !password.trim().isEmpty()) {
            existingUser.setPasswordHash(BcryptUtil.bcryptHash(password)); // Hash the password
        }
    }

    /**
     * Updates an existing user with the provided dto.
     *
     * @param id The ID of the user to update.
     * @param user The DTO containing user profile update data.
     * @return The updated UserDTO.
     * @throws UserNotFoundException If the user with the given ID does not exist.
     * @throws InvalidUserException If the provided data is invalid.
     * @throws DuplicateUserException If a user with the same username or email already exists.
     * @throws RuntimeException If an error occurs while sending email confirmation.
     */
    @Transactional
    public UserDTO updateUser(Long id, AdminUserUpdateDTO user) throws UserNotFoundException {
        User existingUser =
                userRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(
                                                "User with id " + id + " not found."));

        updateUserCore(
                existingUser,
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getPassword());

        if (user.getRoles() != null) {
            existingUser.setRoles(user.getRoles());
        }
        userRepository.persist(existingUser);
        return new UserDTO(existingUser);
    }

    /**
     * Deletes a user by ID.
     *
     * @param id The ID of the user to delete.
     * @throws UserNotFoundException If the user with the given ID does not exist.
     */
    @Transactional
    public void deleteUser(Long id) throws UserNotFoundException {
        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
    }

    public UserDTO getUserById(Long id) {
        User user =
                userRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(
                                                "User with id " + id + " not found."));
        return new UserDTO(user);
    }

    public List<LimitedUserInfoDTO> getAllUsers() {
        return userRepository.listAll().stream().map(LimitedUserInfoDTO::new).toList();
    }

    public List<String> getAvailableRoles() {
        return Arrays.asList(Roles.ALL_ROLES);
    }

    @Transactional
    public UserDTO updateUserProfile(String username, UserProfileUpdateDTO userProfileUpdateDTO)
            throws UserNotFoundException {
        User existingUser =
                userRepository
                        .findByUsernameOptional(username)
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(
                                                "User with username " + username + " not found."));

        updateUserCore(
                existingUser,
                userProfileUpdateDTO.getEmail(),
                userProfileUpdateDTO.getFirstname(),
                userProfileUpdateDTO.getLastname(),
                userProfileUpdateDTO.getPassword());

        userRepository.persist(existingUser);
        return new UserDTO(existingUser);
    }

    /**
     * Verifies the email address of a user using the provided token.
     *
     * @param id The ID of the email verification record.
     * @param token The token to verify.
     * @return The email address of the user if verification is successful.
     * @throws IllegalArgumentException If the ID or token is invalid.
     * @throws TokenExpiredException If the token has expired.
     */
    @Transactional
    public String verifyEmail(Long id, String token) throws TokenExpiredException {
        // Validate id and token
        if (id == null || token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid id or token");
        }

        // Get the email verification record
        EmailVerification emailVerification =
                emailVerificationRepository
                        .findByIdOptional(id)
                        .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        // Check if the token is correct
        if (!emailVerification.getToken().equals(token)) {
            throw new IllegalArgumentException("Invalid token");
        }

        // Check if the token has expired
        if (emailVerification.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token expired");
        }

        // Delete the email verification record
        emailVerificationRepository.deleteById(id);

        // Mark the email as verified
        User user = emailVerification.getUser();
        user.setEmailVerified(true);
        userRepository.persist(user);

        return user.getEmail();
    }
}
