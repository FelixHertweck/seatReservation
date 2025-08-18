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
package de.felixhertweck.seatreservation.eventManagement.ressource;

import java.util.List;
import java.util.Set;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.eventManagement.dto.BlockSeatsRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.DetailedReservationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.service.ReservationService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
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
                                            implementation = DetailedReservationResponseDTO.class)))
    public List<DetailedReservationResponseDTO> getAllReservations() {
        LOG.infof("Received GET request to /api/manager/reservations to get all reservations.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<DetailedReservationResponseDTO> result =
                reservationService.findAllReservations(currentUser);
        LOG.infof(
                "Successfully responded to GET /api/manager/reservations with %d reservations.",
                result.size());
        return result;
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(implementation = DetailedReservationResponseDTO.class)))
    public DetailedReservationResponseDTO getReservationById(@PathParam("id") Long id) {
        LOG.infof("Received GET request to /api/manager/reservations/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        DetailedReservationResponseDTO result =
                reservationService.findReservationById(id, currentUser);
        if (result != null) {
            LOG.infof("Successfully retrieved reservation with ID %d.", id);
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
                                            implementation = DetailedReservationResponseDTO.class)))
    public List<DetailedReservationResponseDTO> getReservationsByEventId(
            @PathParam("id") Long eventId) {
        LOG.infof("Received GET request to /api/manager/reservations/event/%d.", eventId);
        User currentUser = userSecurityContext.getCurrentUser();
        List<DetailedReservationResponseDTO> result =
                reservationService.findReservationsByEventId(eventId, currentUser);
        LOG.infof(
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
                                            implementation = DetailedReservationResponseDTO.class)))
    public Set<DetailedReservationResponseDTO> createReservations(ReservationRequestDTO dto) {
        LOG.infof("Received POST request to /api/manager/reservations to create new reservations.");
        LOG.debugf("ReservationRequestDTO received: %s", dto.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        Set<DetailedReservationResponseDTO> results =
                reservationService.createReservations(dto, currentUser);
        LOG.infof(
                "Reservations created successfully for seat IDs %s and user ID %d.",
                dto.getSeatIds(), dto.getUserId());
        return results;
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "OK")
    public void deleteReservation(@PathParam("id") Long id) {
        LOG.infof(
                "Received DELETE request to /api/manager/reservations/%d to delete reservation.",
                id);
        User currentUser = userSecurityContext.getCurrentUser();
        reservationService.deleteReservation(id, currentUser);
        LOG.infof("Reservation with ID %d deleted successfully.", id);
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
                                            implementation = DetailedReservationResponseDTO.class)))
    public Set<DetailedReservationResponseDTO> blockSeats(BlockSeatsRequestDTO dto) {
        LOG.infof(
                "Received POST request to /api/manager/reservations/block to block seats for event"
                        + " ID %d.",
                dto.getEventId());
        LOG.debugf("BlockSeatsRequestDTO received: %s", dto.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        Set<DetailedReservationResponseDTO> results =
                reservationService.blockSeats(dto.getEventId(), dto.getSeatIds(), currentUser);
        LOG.infof("Seats blocked successfully for event ID %d.", dto.getEventId());
        return results;
    }
}
