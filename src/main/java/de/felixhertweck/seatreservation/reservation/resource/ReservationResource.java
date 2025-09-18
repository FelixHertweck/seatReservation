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
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.reservation.dto.ReservationResponseDTO;
import de.felixhertweck.seatreservation.reservation.dto.ReservationsRequestCreateDTO;
import de.felixhertweck.seatreservation.reservation.service.ReservationService;
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
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    public List<ReservationResponseDTO> getMyReservations() {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
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
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Reservation with specified ID not found for the current user")
    public ReservationResponseDTO getMyReservationById(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
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
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event or seat not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Seat already reserved or event booking closed")
    public List<ReservationResponseDTO> createReservation(@Valid ReservationsRequestCreateDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
                "Received POST request to /api/user/reservations for user: %s",
                currentUser.getUsername());
        List<ReservationResponseDTO> createdReservations =
                reservationService.createReservationForUser(dto, currentUser);
        LOG.debugf(
                "Created %d reservations for user: %s",
                createdReservations.size(), currentUser.getUsername());
        return createdReservations;
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "No Content")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Reservation with specified ID not found for the current user")
    public void deleteReservation(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
                "Received DELETE request to /api/user/reservations/%d for user: %s",
                id, currentUser.getUsername());
        reservationService.deleteReservationForUser(id, currentUser);
        LOG.debugf(
                "Reservation with ID %d deleted successfully for user: %s",
                id, currentUser.getUsername());
    }
}
