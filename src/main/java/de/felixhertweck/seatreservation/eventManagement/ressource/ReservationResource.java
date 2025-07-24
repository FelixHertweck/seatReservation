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

@Path("/api/manager/reservations")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReservationResource {

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
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.findAllReservations(currentUser);
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
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.findReservationById(id, currentUser);
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
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.findReservationsByEventId(eventId, currentUser);
    }

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(implementation = DetailedReservationResponseDTO.class)))
    public DetailedReservationResponseDTO createReservation(ReservationRequestDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.createReservation(dto, currentUser);
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(implementation = DetailedReservationResponseDTO.class)))
    public DetailedReservationResponseDTO updateReservation(
            @PathParam("id") Long id, ReservationRequestDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.updateReservation(id, dto, currentUser);
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "OK")
    public void deleteReservation(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        reservationService.deleteReservation(id, currentUser);
    }

    @POST
    @Path("/block")
    @APIResponse(responseCode = "204", description = "Seats blocked successfully")
    public void blockSeats(BlockSeatsRequestDTO dto) {
        User currentUser = userSecurityContext.getCurrentUser();
        reservationService.blockSeats(dto.getEventId(), dto.getSeatIds(), currentUser);
    }
}
