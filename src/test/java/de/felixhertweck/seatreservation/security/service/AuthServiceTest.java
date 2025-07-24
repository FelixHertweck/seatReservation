package de.felixhertweck.seatreservation.security.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.AuthenticationFailedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class AuthServiceTest {

    @InjectMock UserRepository userRepository;

    @InjectMock TokenService tokenService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        Mockito.reset(userRepository, tokenService);
        authService = new AuthService();
        authService.userRepository = userRepository;
        authService.tokenService = tokenService;
    }

    @Test
    void testAuthenticateSuccess() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "testpassword";
        String passwordHash = BcryptUtil.bcryptHash(password);
        String expectedToken = "mockedToken";

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);

        when(userRepository.findByUsername(username)).thenReturn(user);
        when(tokenService.generateToken(user)).thenReturn(expectedToken);

        String actualToken = authService.authenticate(username, password);

        assertEquals(expectedToken, actualToken);
    }

    @Test
    void testAuthenticateFailureUserNotFound() {
        String username = "nonexistentuser";
        String password = "anypassword";

        when(userRepository.findByUsername(username)).thenReturn(null);

        AuthenticationFailedException thrown =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> authService.authenticate(username, password),
                        "Expected AuthenticationFailedException for user not found");

        assertTrue(thrown.getMessage().contains("Failed to authenticate user: " + username));
    }

    @Test
    void testAuthenticateFailureWrongPassword() {
        String username = "testuser";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";
        String passwordHash = BcryptUtil.bcryptHash(correctPassword);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);

        when(userRepository.findByUsername(username)).thenReturn(user);

        AuthenticationFailedException thrown =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> authService.authenticate(username, wrongPassword),
                        "Expected AuthenticationFailedException for wrong password");

        assertTrue(thrown.getMessage().contains("Failed to authenticate user: " + username));
    }
}
