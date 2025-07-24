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
package de.felixhertweck.seatreservation.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import de.felixhertweck.seatreservation.eventManagement.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.eventManagement.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.eventManagement.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.reservation.EventBookingClosedException;
import de.felixhertweck.seatreservation.reservation.EventNotFoundException;
import de.felixhertweck.seatreservation.reservation.NoSeatsAvailableException;
import de.felixhertweck.seatreservation.reservation.SeatAlreadyReservedException;
import de.felixhertweck.seatreservation.security.AuthenticationFailedException;
import de.felixhertweck.seatreservation.userManagment.exceptions.DuplicateUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.InvalidUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.TokenExpiredException;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(exception.getMessage());
        Response.Status status;

        switch (exception) {
            case EventBookingClosedException ignored -> status = Response.Status.NOT_ACCEPTABLE;
            case EventNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case EventLocationNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case ReservationNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case SeatNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case NoSeatsAvailableException ignored -> status = Response.Status.NOT_ACCEPTABLE;
            case SeatAlreadyReservedException ignored -> status = Response.Status.CONFLICT;
            case AuthenticationFailedException ignored -> status = Response.Status.UNAUTHORIZED;
            case DuplicateUserException ignored -> status = Response.Status.CONFLICT;
            case InvalidUserException ignored -> status = Response.Status.BAD_REQUEST;
            case TokenExpiredException ignored -> status = Response.Status.UNAUTHORIZED;
            case UserNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case IllegalArgumentException ignored -> status = Response.Status.BAD_REQUEST;
            case SecurityException ignored -> status = Response.Status.FORBIDDEN;
            case IllegalStateException ignored -> status = Response.Status.BAD_REQUEST;
            default -> {
                status = Response.Status.INTERNAL_SERVER_ERROR;
                errorResponse =
                        new ErrorResponseDTO(
                                "An unexpected error occurred: " + exception.getMessage());
            }
        }

        return Response.status(status).entity(errorResponse).build();
    }
}
