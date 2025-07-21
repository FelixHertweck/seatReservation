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
        if (exception instanceof EventBookingClosedException) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof EventNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof EventLocationNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof ReservationNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof SeatNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof NoSeatsAvailableException) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof SeatAlreadyReservedException) {
            return Response.status(Response.Status.CONFLICT).entity(exception.getMessage()).build();
        } else if (exception instanceof AuthenticationFailedException) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof DuplicateUserException) {
            return Response.status(Response.Status.CONFLICT).entity(exception.getMessage()).build();
        } else if (exception instanceof InvalidUserException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof TokenExpiredException) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof UserNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(exception.getMessage())
                    .build();
        } else if (exception instanceof SecurityException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(exception.getMessage())
                    .build();
        }
        // Fallback for any other unexpected exceptions
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("An unexpected error occurred: " + exception.getMessage())
                .build();
    }
}
