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
package de.felixhertweck.seatreservation.management.resource;

import java.util.Collections;
import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.management.service.SeatService;
import de.felixhertweck.seatreservation.model.entity.Roles;
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
        SeatDTO result = seatService.createSeatManager(seatRequestDTO, currentUser);
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
        List<SeatDTO> result = seatService.findAllSeatsForManager(currentUser);
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
        SeatDTO result = seatService.findSeatByIdForManager(id, currentUser);
        if (result != null) {
            LOG.debugf("Successfully retrieved seat with ID %d.", id);
        } else {
            LOG.warnf("Seat with ID %d not found.", id);
        }
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
        SeatDTO result = seatService.updateSeatForManager(id, seatUpdateDTO, currentUser);
        LOG.debugf("Seat with ID %d updated successfully.", id);
        return result;
    }

    @DELETE
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "204", description = "Seat deleted successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid input")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Seat with specified ID not found for the current manager")
    public void deleteManagerSeat(@QueryParam("ids") List<Long> ids) {
        LOG.debugf(
                "Received DELETE request to /api/manager/seats with IDs: %s",
                ids != null ? ids : Collections.emptyList());
        User currentUser = userSecurityContext.getCurrentUser();
        seatService.deleteSeatForManager(ids, currentUser);
        LOG.debugf(
                "Seats with IDs %s deleted successfully.",
                ids != null ? ids : Collections.emptyList());
    }
}
