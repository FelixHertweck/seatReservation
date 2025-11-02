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

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import de.felixhertweck.seatreservation.common.dto.ErrorResponseDTO;
import de.felixhertweck.seatreservation.common.exception.DuplicateUserException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.InvalidUserException;
import de.felixhertweck.seatreservation.common.exception.RegistrationDisabledException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.management.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.management.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.management.exception.SeatNotFoundException;
import de.felixhertweck.seatreservation.reservation.exception.EventBookingClosedException;
import de.felixhertweck.seatreservation.reservation.exception.NoSeatsAvailableException;
import de.felixhertweck.seatreservation.reservation.exception.SeatAlreadyReservedException;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.security.exceptions.JwtInvalidException;
import de.felixhertweck.seatreservation.security.service.TokenService;
import de.felixhertweck.seatreservation.userManagment.exceptions.VerificationCodeNotFoundException;
import de.felixhertweck.seatreservation.userManagment.exceptions.VerifyTokenExpiredException;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Inject TokenService tokenService;

    @Override
    public Response toResponse(Exception exception) {
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(exception.getMessage());
        Response.Status status;
        List<NewCookie> cookies = new ArrayList<>();

        switch (exception) {
            case EventBookingClosedException ignored -> status = Response.Status.BAD_REQUEST;
            case EventNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case EventLocationNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case ReservationNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case SeatNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case NoSeatsAvailableException ignored -> status = Response.Status.BAD_REQUEST;
            case SeatAlreadyReservedException ignored -> status = Response.Status.CONFLICT;
            case JwtInvalidException ignored -> {
                NewCookie jwtAccessCookie = tokenService.createNewNullCookie("jwt", true);
                NewCookie refreshTokenCookie =
                        tokenService.createNewNullCookie("refreshToken", true);
                NewCookie refreshTokenExpirationCookie =
                        tokenService.createNewNullCookie("refreshToken_expiration", false);
                cookies.add(jwtAccessCookie);
                cookies.add(refreshTokenCookie);
                cookies.add(refreshTokenExpirationCookie);
                status = Response.Status.UNAUTHORIZED;
            }
            case AuthenticationFailedException ignored -> status = Response.Status.UNAUTHORIZED;
            case DuplicateUserException ignored -> status = Response.Status.CONFLICT;
            case InvalidUserException ignored -> status = Response.Status.BAD_REQUEST;
            case RegistrationDisabledException ignored -> status = Response.Status.FORBIDDEN;
            case VerificationCodeNotFoundException ignored -> status = Response.Status.BAD_REQUEST;
            case VerifyTokenExpiredException ignored -> status = Response.Status.GONE;
            case UserNotFoundException ignored -> status = Response.Status.NOT_FOUND;
            case IllegalArgumentException ignored -> status = Response.Status.BAD_REQUEST;
            case SecurityException ignored -> status = Response.Status.FORBIDDEN;
            case IllegalStateException ignored -> status = Response.Status.BAD_REQUEST;
            default -> {
                status = Response.Status.INTERNAL_SERVER_ERROR;
            }
        }

        LOG.warnf(
                "Exception occurred (handled by GlobalExceptionHandler): %s", exception.toString());

        return Response.status(status)
                .entity(errorResponse)
                .cookie(cookies.toArray(NewCookie[]::new))
                .build();
    }
}
