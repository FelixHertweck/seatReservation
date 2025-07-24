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

@Path("/api/user/reservations")
@RolesAllowed({Roles.USER, Roles.MANAGER, Roles.ADMIN})
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
                                            implementation = ReservationResponseDTO.class)))
    public List<ReservationResponseDTO> getMyReservations() {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.findReservationsByUser(currentUser);
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ReservationResponseDTO.class)))
    public ReservationResponseDTO getMyReservationById(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        return reservationService.findReservationByIdForUser(id, currentUser);
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
        return reservationService.createReservationForUser(dto, currentUser);
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
        reservationService.deleteReservationForUser(id, currentUser);
    }
}
