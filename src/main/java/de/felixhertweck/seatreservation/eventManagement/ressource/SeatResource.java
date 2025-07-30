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
            content = @Content(schema = @Schema(implementation = SeatResponseDTO.class)))
    public SeatResponseDTO createSeat(SeatRequestDTO seatRequestDTO) {
        LOG.infof("Received POST request to /api/manager/seats to create a new seat.");
        LOG.debugf("SeatRequestDTO received: %s", seatRequestDTO.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        SeatResponseDTO result = seatService.createSeatManager(seatRequestDTO, currentUser);
        LOG.infof(
                "Seat with ID %d created successfully for event location ID %d.",
                result.id(), result.eventLocationId());
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
                                            implementation = SeatResponseDTO.class)))
    public List<SeatResponseDTO> getAllManagerSeats() {
        LOG.infof("Received GET request to /api/manager/seats to get all manager seats.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<SeatResponseDTO> result = seatService.findAllSeatsForManager(currentUser);
        LOG.infof("Successfully responded to GET /api/manager/seats with %d seats.", result.size());
        return result;
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SeatResponseDTO.class)))
    public SeatResponseDTO getManagerSeatById(@PathParam("id") Long id) {
        LOG.infof("Received GET request to /api/manager/seats/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        SeatResponseDTO result = seatService.findSeatByIdForManager(id, currentUser);
        if (result != null) {
            LOG.infof("Successfully retrieved seat with ID %d.", id);
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
            content = @Content(schema = @Schema(implementation = SeatResponseDTO.class)))
    public SeatResponseDTO updateManagerSeat(
            @PathParam("id") Long id, SeatRequestDTO seatUpdateDTO) {
        LOG.infof("Received PUT request to /api/manager/seats/%d to update seat.", id);
        LOG.debugf("SeatRequestDTO received for ID %d: %s", id, seatUpdateDTO.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        SeatResponseDTO result = seatService.updateSeatForManager(id, seatUpdateDTO, currentUser);
        LOG.infof("Seat with ID %d updated successfully.", id);
        return result;
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "OK")
    public void deleteManagerSeat(@PathParam("id") Long id) {
        LOG.infof("Received DELETE request to /api/manager/seats/%d to delete seat.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        seatService.deleteSeatForManager(id, currentUser);
        LOG.infof("Seat with ID %d deleted successfully.", id);
    }
}
