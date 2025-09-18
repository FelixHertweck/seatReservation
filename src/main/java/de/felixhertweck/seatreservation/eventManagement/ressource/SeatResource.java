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
import java.util.stream.Collectors;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.service.SeatService;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/seats")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeatResource {

    private static final Logger LOG = Logger.getLogger(SeatResource.class);

    @Inject SeatService seatService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SeatDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event location not found")
    @APIResponse(
            responseCode = "409",
            description =
                    "Conflict: Seat with this row and number already exists in this event location")
    public SeatDTO createSeat(@Valid SeatRequestDTO seatRequestDTO) {
        LOG.debugf("Received POST request to /api/manager/seats to create a new seat.");
        User currentUser = userSecurityContext.getCurrentUser();
        Seat seat = seatService.createSeatManager(seatRequestDTO, currentUser);
        SeatDTO result = SeatDTO.toDTO(seat);
        LOG.debugf(
                "Seat with ID %d created successfully for event location ID %d.",
                result.id(), result.locationId());
        return result;
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
                                            implementation = SeatDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<SeatDTO> getAllManagerSeats() {
        LOG.debugf("Received GET request to /api/manager/seats to get all manager seats.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<Seat> seats = seatService.findAllSeatsForManager(currentUser);
        List<SeatDTO> result = seats.stream().map(SeatDTO::toDTO).collect(Collectors.toList());
        LOG.debugf(
                "Successfully responded to GET /api/manager/seats with %d seats.", result.size());
        return result;
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SeatDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Seat with specified ID not found for the current manager")
    public SeatDTO getManagerSeatById(@PathParam("id") Long id) {
        LOG.debugf("Received GET request to /api/manager/seats/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        Seat seat = seatService.findSeatByIdForManager(id, currentUser);
        SeatDTO result = SeatDTO.toDTO(seat);
        LOG.debugf("Successfully retrieved seat with ID %d.", id);
        return result;
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SeatDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Seat with specified ID not found for the current manager")
    @APIResponse(
            responseCode = "409",
            description =
                    "Conflict: Seat with this row and number already exists in this event location")
    public SeatDTO updateManagerSeat(
            @PathParam("id") Long id, @Valid SeatRequestDTO seatUpdateDTO) {
        LOG.debugf("Received PUT request to /api/manager/seats/%d to update seat.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        Seat seat = seatService.updateSeatForManager(id, seatUpdateDTO, currentUser);
        SeatDTO result = SeatDTO.toDTO(seat);
        LOG.debugf("Seat with ID %d updated successfully.", id);
        return result;
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "204", description = "Seat deleted successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Seat with specified ID not found for the current manager")
    public void deleteManagerSeat(@PathParam("id") Long id) {
        LOG.debugf("Received DELETE request to /api/manager/seats/%d to delete seat.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        seatService.deleteSeatForManager(id, currentUser);
        LOG.debugf("Seat with ID %d deleted successfully.", id);
    }
}
