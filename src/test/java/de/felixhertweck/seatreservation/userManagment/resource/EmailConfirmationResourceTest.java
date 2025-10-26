/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package de.felixhertweck.seatreservation.userManagment.resource;

import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.userManagment.dto.VerifyEmailCodeRequestDto;
import de.felixhertweck.seatreservation.userManagment.exceptions.VerificationCodeNotFoundException;
import de.felixhertweck.seatreservation.userManagment.exceptions.VerifyTokenExpiredException;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EmailConfirmationResourceTest {

    @InjectMock UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void verifyEmailWithCode_Success() throws Exception {
        // Given
        VerifyEmailCodeRequestDto request = new VerifyEmailCodeRequestDto("123456");
        when(userService.verifyEmailWithCode("123456")).thenReturn("test@example.com");

        // When & Then
        given().contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/user/verify-email-code")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("message", containsString("Email verified successfully"))
                .body("email", containsString("test@example.com"));

        verify(userService, times(1)).verifyEmailWithCode("123456");
    }

    @Test
    void verifyEmailWithCode_BadRequest_InvalidCode() throws Exception {
        // Given
        VerifyEmailCodeRequestDto request = new VerifyEmailCodeRequestDto("123456");
        when(userService.verifyEmailWithCode("123456"))
                .thenThrow(new VerificationCodeNotFoundException("Verification code not found"));

        // When & Then
        given().contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/user/verify-email-code")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("message", containsString("Verification code not found"));

        verify(userService, times(1)).verifyEmailWithCode("123456");
    }

    @Test
    void verifyEmailWithCode_Gone_ExpiredCode() throws Exception {
        // Given
        VerifyEmailCodeRequestDto request = new VerifyEmailCodeRequestDto("123456");
        when(userService.verifyEmailWithCode("123456"))
                .thenThrow(new VerifyTokenExpiredException("Verification code expired"));

        // When & Then
        given().contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/user/verify-email-code")
                .then()
                .statusCode(Response.Status.GONE.getStatusCode())
                .body(containsString("Verification code expired"));

        verify(userService, times(1)).verifyEmailWithCode("123456");
    }

    @Test
    void verifyEmailWithCode_BadRequest_InvalidFormat() throws Exception {
        // Given - 5 digits instead of 6
        VerifyEmailCodeRequestDto request = new VerifyEmailCodeRequestDto("12345");

        // When & Then
        given().contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/user/verify-email-code")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // UserService should not be called for invalid format
        verify(userService, never()).verifyEmailWithCode(anyString());
    }

    @Test
    void verifyEmailWithCode_BadRequest_EmptyCode() throws Exception {
        // Given
        VerifyEmailCodeRequestDto request = new VerifyEmailCodeRequestDto("");

        // When & Then
        given().contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/user/verify-email-code")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // UserService should not be called for empty code
        verify(userService, never()).verifyEmailWithCode(anyString());
    }

    @Test
    void verifyEmailWithCode_BadRequest_NullCode() throws Exception {
        // Given
        VerifyEmailCodeRequestDto request = new VerifyEmailCodeRequestDto(null);

        // When & Then
        given().contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/user/verify-email-code")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // UserService should not be called for null code
        verify(userService, never()).verifyEmailWithCode(anyString());
    }

    @Test
    void verifyEmailWithCode_InternalServerError() throws Exception {
        // Given
        VerifyEmailCodeRequestDto request = new VerifyEmailCodeRequestDto("123456");
        when(userService.verifyEmailWithCode("123456"))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        given().contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/user/verify-email-code")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body(containsString("Database error"));

        verify(userService, times(1)).verifyEmailWithCode("123456");
    }
}
