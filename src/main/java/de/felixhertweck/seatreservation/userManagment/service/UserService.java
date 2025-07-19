package de.felixhertweck.seatreservation.userManagment.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.email.EmailService;
import de.felixhertweck.seatreservation.model.entity.EmailVerification;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.DuplicateUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.InvalidUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.TokenExpiredException;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;
import io.quarkus.elytron.security.common.BcryptUtil;

// TODO: Add Throws

@ApplicationScoped
public class UserService {

    @Inject UserRepository userRepository;

    @Inject EmailService emailService;

    @Inject EmailVerificationRepository emailVerificationRepository;

    @Transactional
    public UserDTO createUser(UserCreationDTO userCreationDTO) {
        if (userCreationDTO == null
                || userCreationDTO.getUsername() == null
                || userCreationDTO.getUsername().trim().isEmpty()
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
            System.out.println("Sending email confirmation to: " + user.getEmail());
            // Send email confirmation
            try {
                emailService.sendEmailConfirmation(user);
            } catch (IOException e) {
                throw new InternalServerErrorException(
                        "Failed to send email confirmation: " + e.getMessage());
            }
        }

        return new UserDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserProfileUpdateDTO user) {
        User existingUser = userRepository.findById(id);
        if (existingUser == null) {
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
        if (user == null) {
            throw new InvalidUserException("User data cannot be null.");
        }

        // Check for duplicate username if changed
        if (userRepository.findByUsername(existingUser.getUsername()) != null) {
            throw new DuplicateUserException(
                    "User with username " + existingUser.getUsername() + " already exists.");
        }

        // Check for duplicate email if changed
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            existingUser.setEmail(user.getEmail());
            // Reset email verification status and send confirmation email
            existingUser.setEmailVerified(false);
            try {
                emailService.sendEmailConfirmation(existingUser);
            } catch (IOException e) {
                throw new InternalServerErrorException(
                        "Failed to send email confirmation: " + e.getMessage());
            }
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
        return new UserDTO(existingUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
        return new UserDTO(user);
    }

    public List<LimitedUserInfoDTO> getAllUsers() {
        return userRepository.listAll().stream().map(LimitedUserInfoDTO::new).toList();
    }

    public List<String> getAvailableRoles() {
        return Arrays.asList(Roles.ALL_ROLES);
    }

    @Transactional
    public UserDTO updateUserProfile(String username, UserProfileUpdateDTO userProfileUpdateDTO) {
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
            // Reset email verification status and send confirmation email
            existingUser.setEmailVerified(false);
            try {
                emailService.sendEmailConfirmation(existingUser);
            } catch (IOException e) {
                throw new InternalServerErrorException(
                        "Failed to send email confirmation: " + e.getMessage());
            }
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
        if (userProfileUpdateDTO.getPasswordHash() != null
                && !userProfileUpdateDTO.getPasswordHash().trim().isEmpty()) {
            existingUser.setPasswordHash(
                    BcryptUtil.bcryptHash(
                            userProfileUpdateDTO.getPasswordHash())); // Hash the password
        }

        userRepository.persist(existingUser);
        return new UserDTO(existingUser);
    }

    @Transactional
    public String verifyEmail(Long id, String token) throws BadRequestException, NotFoundException {
        // Validate id and token
        if (id == null || token == null || token.isEmpty()) {
            throw new BadRequestException("Invalid id or token");
        }

        // Get the email verification record
        EmailVerification emailVerification = emailVerificationRepository.findById(id);
        if (emailVerification == null) {
            throw new NotFoundException("Token not found");
        }

        // Check if the token is correct
        if (!emailVerification.getToken().equals(token)) {
            throw new BadRequestException("Invalid token");
        }

        // Check if the token has expired
        if (emailVerification.getExpirationTime().isBefore(java.time.LocalDateTime.now())) {
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
