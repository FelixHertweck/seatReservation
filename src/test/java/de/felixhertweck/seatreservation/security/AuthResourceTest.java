package de.felixhertweck.seatreservation.security;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.felixhertweck.seatreservation.security.dto.LoginRequestDTO;
import de.felixhertweck.seatreservation.security.dto.LoginResponseDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class AuthResourceTest {

    @InjectMock AuthService authService;

    @BeforeEach
    void setUp() {
        Mockito.reset(authService);
    }

    @Test
    void testLoginSuccess() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "testpassword";
        String token = "mockedToken123";

        Mockito.when(authService.authenticate(username, password)).thenReturn(token);

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        LoginResponseDTO response =
                given().contentType(MediaType.APPLICATION_JSON)
                        .body(loginRequest)
                        .when()
                        .post("/api/auth/login")
                        .then()
                        .statusCode(Response.Status.OK.getStatusCode())
                        .extract()
                        .as(LoginResponseDTO.class);

        assertEquals(token, response.token());
    }

    @Test
    void testLoginFailureWrongCredentials() throws AuthenticationFailedException {
        String username = "testuser";
        String password = "wrongpassword";
        String errorMessage = "Failed to authenticate user: " + username;

        Mockito.when(authService.authenticate(username, password))
                .thenThrow(new AuthenticationFailedException(errorMessage));

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode())
                .body(org.hamcrest.Matchers.equalTo(errorMessage));
    }

    @Test
    void login_BadRequest_MissingCredentials() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(null); // Explicitly set to null
        loginRequest.setPassword(null); // Explicitly set to null

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        LoginRequestDTO loginRequestEmpty = new LoginRequestDTO();
        loginRequestEmpty.setUsername(""); // Empty username
        loginRequestEmpty.setPassword("password");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequestEmpty)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        LoginRequestDTO loginRequestEmpty2 = new LoginRequestDTO();
        loginRequestEmpty2.setUsername("username");
        loginRequestEmpty2.setPassword(""); // Empty password

        given().contentType(MediaType.APPLICATION_JSON)
                .body(loginRequestEmpty2)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
