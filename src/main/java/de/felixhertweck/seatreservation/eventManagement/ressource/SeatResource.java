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

import de.felixhertweck.seatreservation.eventManagement.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.SeatResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.service.SeatService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/manager/seats")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeatResource {

    @Inject SeatService seatService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SeatResponseDTO.class)))
    public SeatResponseDTO createSeat(SeatRequestDTO seatRequestDTO) {
        User currentUser = userSecurityContext.getCurrentUser();
        return seatService.createSeatManager(seatRequestDTO, currentUser);
    }

    @GET
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = SeatResponseDTO.class)))
    public List<SeatResponseDTO> getAllManagerSeats() {
        User currentUser = userSecurityContext.getCurrentUser();
        return seatService.findAllSeatsForManager(currentUser);
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SeatResponseDTO.class)))
    public SeatResponseDTO getManagerSeatById(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        return seatService.findSeatByIdForManager(id, currentUser);
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SeatResponseDTO.class)))
    public SeatResponseDTO updateManagerSeat(
            @PathParam("id") Long id, SeatRequestDTO seatUpdateDTO) {
        User currentUser = userSecurityContext.getCurrentUser();
        return seatService.updateSeatForManager(id, seatUpdateDTO, currentUser);
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "OK")
    public void deleteManagerSeat(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        seatService.deleteSeatForManager(id, currentUser);
    }
}
