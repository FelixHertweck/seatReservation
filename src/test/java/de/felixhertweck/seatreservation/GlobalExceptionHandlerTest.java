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
package de.felixhertweck.seatreservation;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

import de.felixhertweck.seatreservation.common.dto.ErrorResponseDTO;
import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.management.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.management.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.reservation.exception.EventBookingClosedException;
import de.felixhertweck.seatreservation.reservation.exception.NoSeatsAvailableException;
import de.felixhertweck.seatreservation.reservation.exception.SeatAlreadyReservedException;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import de.felixhertweck.seatreservation.userManagment.exceptions.VerifyTokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testUserNotFoundException() {
        UserNotFoundException exception = new UserNotFoundException("User not found");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("User not found", errorResponse.getMessage());
    }

    @Test
    void testEventNotFoundException() {
        EventNotFoundException exception = new EventNotFoundException("Event not found");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Event not found", errorResponse.getMessage());
    }

    @Test
    void testSeatNotFoundException() {
        SeatNotFoundException exception = new SeatNotFoundException("Seat not found");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Seat not found", errorResponse.getMessage());
    }

    @Test
    void testReservationNotFoundException() {
        ReservationNotFoundException exception =
                new ReservationNotFoundException("Reservation not found");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Reservation not found", errorResponse.getMessage());
    }

    @Test
    void testEventLocationNotFoundException() {
        EventLocationNotFoundException exception =
                new EventLocationNotFoundException("Location not found");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Location not found", errorResponse.getMessage());
    }

    @Test
    void testDuplicateUserException() {
        DuplicateUserException exception = new DuplicateUserException("User already exists");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("User already exists", errorResponse.getMessage());
    }

    @Test
    void testSeatAlreadyReservedException() {
        SeatAlreadyReservedException exception =
                new SeatAlreadyReservedException("Seat already reserved");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Seat already reserved", errorResponse.getMessage());
    }

    @Test
    void testAuthenticationFailedException() {
        AuthenticationFailedException exception =
                new AuthenticationFailedException("Authentication failed");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Authentication failed", errorResponse.getMessage());
    }

    @Test
    void testTokenExpiredException() {
        VerifyTokenExpiredException exception = new VerifyTokenExpiredException("Token expired");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.GONE.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Token expired", errorResponse.getMessage());
    }

    @Test
    void testInvalidUserException() {
        InvalidUserException exception = new InvalidUserException("Invalid user data");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Invalid user data", errorResponse.getMessage());
    }

    @Test
    void testEventBookingClosedException() {
        EventBookingClosedException exception = new EventBookingClosedException("Booking closed");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Booking closed", errorResponse.getMessage());
    }

    @Test
    void testNoSeatsAvailableException() {
        NoSeatsAvailableException exception = new NoSeatsAvailableException("No seats available");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("No seats available", errorResponse.getMessage());
    }

    @Test
    void testGenericException() {
        RuntimeException exception = new RuntimeException("Generic error");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Generic error", errorResponse.getMessage());
    }

    @Test
    void testNullPointerException() {
        NullPointerException exception = new NullPointerException("Null pointer");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("Null pointer", errorResponse.getMessage());
    }

    @Test
    void testExceptionWithNullMessage() {
        RuntimeException exception = new RuntimeException((String) null);
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertNull(errorResponse.getMessage());
    }

    @Test
    void testJwtInvalidException() {
        JwtInvalidException exception = new JwtInvalidException("JWT token is invalid");
        Response response = exceptionHandler.toResponse(exception);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorResponseDTO);
        ErrorResponseDTO errorResponse = (ErrorResponseDTO) response.getEntity();
        assertEquals("JWT token is invalid", errorResponse.getMessage());
    }
}
