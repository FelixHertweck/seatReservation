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
package de.felixhertweck.seatreservation.supervisor.resource;

import java.util.List;
import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.supervisor.dto.GuestSeatAssignRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.GuestSeatAssignmentResponseDTO;
import de.felixhertweck.seatreservation.supervisor.service.GuestSeatAssignmentService;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/supervisor/liveview/guest-assignments")
@RolesAllowed({Roles.SUPERVISOR, Roles.ADMIN, Roles.MANAGER})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GuestSeatAssignmentResource {

    private static final Logger LOG = Logger.getLogger(GuestSeatAssignmentResource.class);

    @Inject GuestSeatAssignmentService guestSeatAssignmentService;
    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "201",
            description = "Guest seat assignments created successfully",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            implementation =
                                                    GuestSeatAssignmentResponseDTO[].class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "400", description = "Bad Request")
    public Response assignGuestSeats(@Valid GuestSeatAssignRequestDTO requestDTO) {
        LOG.infof(
                "Received request for guest seat assignment on event %s with %d seats",
                requestDTO.eventId, requestDTO.seatIds.size());

        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        List<GuestSeatAssignmentResponseDTO> response =
                guestSeatAssignmentService.assignSeats(requestDTO, currentUser);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "Guest seat assignment removed successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "404", description = "Assignment not found")
    public Response removeGuestAssignment(@PathParam("id") UUID id) {
        LOG.infof("Received request to remove guest seat assignment %s", id);

        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        guestSeatAssignmentService.removeAssignment(id, currentUser);

        return Response.noContent().build();
    }
}
