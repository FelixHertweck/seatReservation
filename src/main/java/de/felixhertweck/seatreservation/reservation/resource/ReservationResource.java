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
package de.felixhertweck.seatreservation.reservation.resource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.reservation.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.reservation.dto.ReservationsRequestCreateDTO;
import de.felixhertweck.seatreservation.reservation.service.ReservationService;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/user/reservations")
@RolesAllowed({Roles.USER, Roles.MANAGER, Roles.ADMIN})
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
    public List<ReservationResponseDTO> getMyReservations() {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.infof(
                "Received GET request to /api/user/reservations for user: %s",
                currentUser.getUsername());
        List<ReservationResponseDTO> reservations =
                reservationService.findReservationsByUser(currentUser);
        LOG.debugf(
                "Returning %d reservations for user: %s",
                reservations.size(), currentUser.getUsername());
        return reservations;
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ReservationResponseDTO.class)))
    public ReservationResponseDTO getMyReservationById(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.infof(
                "Received GET request to /api/user/reservations/%d for user: %s",
                id, currentUser.getUsername());
        ReservationResponseDTO reservation =
                reservationService.findReservationByIdForUser(id, currentUser);
        LOG.debugf("Returning reservation with ID %d for user: %s", id, currentUser.getUsername());
        return reservation;
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
    public List<ReservationResponseDTO> createReservation(ReservationsRequestCreateDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.infof(
                "Received POST request to /api/user/reservations for user: %s",
                currentUser.getUsername());
        LOG.debugf(
                "ReservationsRequestCreateDTO received for user %s: %s",
                currentUser.getUsername(), dto.toString());
        List<ReservationResponseDTO> createdReservations =
                reservationService.createReservationForUser(dto, currentUser);
        LOG.infof(
                "Created %d reservations for user: %s",
                createdReservations.size(), currentUser.getUsername());
        return createdReservations;
    }

    // Maybe remove this method in the future and only allow creation and deletion of reservations
    /*@PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = DetailedReservationResponseDTO.class)))
    public DetailedReservationResponseDTO updateReservation(
            @PathParam("id") Long id, ReservationRequestUpdateDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.updateReservationForUser(id, dto, currentUser);
    }*/

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "No Content")
    public void deleteReservation(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.infof(
                "Received DELETE request to /api/user/reservations/%d for user: %s",
                id, currentUser.getUsername());
        reservationService.deleteReservationForUser(id, currentUser);
        LOG.infof(
                "Reservation with ID %d deleted successfully for user: %s",
                id, currentUser.getUsername());
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
    public Response exportReservationsToCsv(@PathParam("eventId") Long eventId) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.infof(
                "Received GET request to /api/user/reservations/export/%d/csv for user: %s",
                eventId, currentUser.getUsername());
        try {
            byte[] csvData = reservationService.exportReservationsToCsv(eventId, currentUser);
            LOG.infof(
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
        } catch (de.felixhertweck.seatreservation.reservation.EventNotFoundException e) {
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
}
