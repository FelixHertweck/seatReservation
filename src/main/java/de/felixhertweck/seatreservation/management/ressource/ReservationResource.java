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
package de.felixhertweck.seatreservation.management.ressource;

import java.util.List;
import java.util.Set;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.management.dto.BlockSeatsRequestDTO;
import de.felixhertweck.seatreservation.management.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.management.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.management.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/reservations")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReservationResource {

    private static final Logger LOG = Logger.getLogger(ReservationResource.class);

    @Inject ReservationService reservationService;

    @Inject UserSecurityContext userSecurityContext;

    @GET
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = ReservationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<ReservationResponseDTO> getAllReservations() {
        LOG.debugf("Received GET request to /api/manager/reservations to get all reservations.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<ReservationResponseDTO> result = reservationService.findAllReservations(currentUser);
        LOG.debugf(
                "Successfully responded to GET /api/manager/reservations with %d reservations.",
                result.size());
        return result;
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ReservationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description =
                    "Not Found: Reservation with specified ID not found for the current manager")
    public ReservationResponseDTO getReservationById(@PathParam("id") Long id) {
        LOG.debugf("Received GET request to /api/manager/reservations/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        ReservationResponseDTO result = reservationService.findReservationById(id, currentUser);
        if (result != null) {
            LOG.debugf("Successfully retrieved reservation with ID %d.", id);
        } else {
            LOG.warnf("Reservation with ID %d not found.", id);
        }
        return result;
    }

    @GET
    @Path("/event/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = ReservationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Event with specified ID not found for the current manager")
    public List<ReservationResponseDTO> getReservationsByEventId(@PathParam("id") Long eventId) {
        LOG.debugf("Received GET request to /api/manager/reservations/event/%d.", eventId);
        User currentUser = userSecurityContext.getCurrentUser();
        List<ReservationResponseDTO> result =
                reservationService.findReservationsByEventId(eventId, currentUser);
        LOG.debugf(
                "Successfully retrieved %d reservations for event ID %d.", result.size(), eventId);
        return result;
    }

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = ReservationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event, user or seat not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Seat already reserved or event booking closed")
    public Set<ReservationResponseDTO> createReservations(@Valid ReservationRequestDTO dto) {
        LOG.debugf(
                "Received POST request to /api/manager/reservations to create new reservations.");
        User currentUser = userSecurityContext.getCurrentUser();
        Set<ReservationResponseDTO> results =
                reservationService.createReservations(dto, currentUser);
        LOG.debugf(
                "Reservations created successfully for seat IDs %s and user ID %d.",
                dto.getSeatIds(), dto.getUserId());
        return results;
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "Reservation deleted successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description =
                    "Not Found: Reservation with specified ID not found for the current manager")
    public void deleteReservation(@PathParam("id") Long id) {
        LOG.debugf(
                "Received DELETE request to /api/manager/reservations/%d to delete reservation.",
                id);
        User currentUser = userSecurityContext.getCurrentUser();
        reservationService.deleteReservation(id, currentUser);
        LOG.debugf("Reservation with ID %d deleted successfully.", id);
    }

    @POST
    @Path("/block")
    @APIResponse(
            responseCode = "200",
            description = "Seats blocked successfully",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = ReservationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event or seat not found")
    @APIResponse(responseCode = "409", description = "Conflict: Seat already blocked")
    public Set<ReservationResponseDTO> blockSeats(@Valid BlockSeatsRequestDTO dto) {
        LOG.debugf(
                "Received POST request to /api/manager/reservations/block to block seats for event"
                        + " ID %d.",
                dto.getEventId());
        User currentUser = userSecurityContext.getCurrentUser();
        Set<ReservationResponseDTO> results =
                reservationService.blockSeats(dto.getEventId(), dto.getSeatIds(), currentUser);
        LOG.debugf("Seats blocked successfully for event ID %d.", dto.getEventId());
        return results;
    }

    @GET
    @Path("/export/{eventId}/csv")
    @RolesAllowed({Roles.MANAGER, Roles.ADMIN})
    @Produces("text/csv")
    @APIResponse(
            responseCode = "200",
            description = "CSV export of reservations for a specific event",
            content = @Content(mediaType = "text/csv"))
    @APIResponse(responseCode = "403", description = "Forbidden - User not authorized")
    @APIResponse(responseCode = "404", description = "Not Found - Event not found")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "500", description = "Internal Server Error during CSV export")
    public Response exportReservationsToCsv(@PathParam("eventId") Long eventId) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
                "Received GET request to /api/manager/reservations/export/%d/csv for user: %s",
                eventId, currentUser.getUsername());
        try {
            byte[] csvData = reservationService.exportReservationsToCsv(eventId, currentUser);
            LOG.debugf(
                    "Successfully exported CSV for event ID %d for user: %s",
                    eventId, currentUser.getUsername());
            return Response.ok(csvData)
                    .header(
                            "Content-Disposition",
                            "attachment; filename=\"reservations_event_" + eventId + ".csv\"")
                    .build();
        } catch (SecurityException e) {
            LOG.warnf(
                    "User %s (ID: %d) attempted unauthorized CSV export for event ID %d: %s",
                    currentUser.getUsername(), currentUser.getId(), eventId, e.getMessage());
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (EventNotFoundException e) {
            LOG.warnf(
                    "Event ID %d not found for CSV export requested by user %s (ID: %d): %s",
                    eventId, currentUser.getUsername(), currentUser.getId(), e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to export CSV for event ID %d for user %s (ID: %d)",
                    eventId,
                    currentUser.getUsername(),
                    currentUser.getId());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal server error during CSV export")
                    .build();
        }
    }

    @GET
    @Path("/export/{eventId}/pdf")
    @RolesAllowed({Roles.MANAGER, Roles.ADMIN})
    @Produces("application/pdf")
    @APIResponse(
            responseCode = "200",
            description = "PDF export of reservations for a specific event",
            content = @Content(mediaType = "application/pdf"))
    @APIResponse(responseCode = "403", description = "Forbidden - User not authorized")
    @APIResponse(responseCode = "404", description = "Not Found - Event not found")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "500", description = "Internal Server Error during PDF export")
    public Response exportReservationsToPdf(@PathParam("eventId") Long eventId) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
                "Received GET request to /api/manager/reservations/export/%d/pdf for user: %s",
                eventId, currentUser.getUsername());
        try {
            byte[] pdfData = reservationService.exportReservationsToPdf(eventId, currentUser);
            LOG.debugf(
                    "Successfully exported PDF for event ID %d for user: %s",
                    eventId, currentUser.getUsername());
            return Response.ok(pdfData)
                    .header(
                            "Content-Disposition",
                            "attachment; filename=\"reservations_event_" + eventId + ".pdf\"")
                    .build();
        } catch (SecurityException e) {
            LOG.warnf(
                    "User %s (ID: %d) attempted unauthorized PDF export for event ID %d: %s",
                    currentUser.getUsername(), currentUser.getId(), eventId, e.getMessage());
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (EventNotFoundException e) {
            LOG.warnf(
                    "Event ID %d not found for PDF export requested by user %s (ID: %d): %s",
                    eventId, currentUser.getUsername(), currentUser.getId(), e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to export CSV for event ID %d for user %s (ID: %d)",
                    eventId,
                    currentUser.getUsername(),
                    currentUser.getId());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal server error during CSV export")
                    .build();
        }
    }
}
