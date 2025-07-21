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

        if (exception instanceof EventBookingClosedException) {
            status = Response.Status.NOT_ACCEPTABLE;
        } else if (exception instanceof EventNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (exception instanceof EventLocationNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (exception instanceof ReservationNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (exception instanceof SeatNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (exception instanceof NoSeatsAvailableException) {
            status = Response.Status.NOT_ACCEPTABLE;
        } else if (exception instanceof SeatAlreadyReservedException) {
            status = Response.Status.CONFLICT;
        } else if (exception instanceof AuthenticationFailedException) {
            status = Response.Status.UNAUTHORIZED;
        } else if (exception instanceof DuplicateUserException) {
            status = Response.Status.CONFLICT;
        } else if (exception instanceof InvalidUserException) {
            status = Response.Status.BAD_REQUEST;
        } else if (exception instanceof TokenExpiredException) {
            status = Response.Status.UNAUTHORIZED;
        } else if (exception instanceof UserNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (exception instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST;
        } else if (exception instanceof SecurityException) {
            status = Response.Status.FORBIDDEN;
        } else if (exception instanceof IllegalStateException) {
            status = Response.Status.BAD_REQUEST;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            errorResponse =
                    new ErrorResponseDTO("An unexpected error occurred: " + exception.getMessage());
        }

        return Response.status(status).entity(errorResponse).build();
    }
}
