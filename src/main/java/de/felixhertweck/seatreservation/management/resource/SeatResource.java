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

import java.util.List;
import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.management.service.SeatService;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
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
    @APIResponse(responseCode = "400", description = "Bad Request: eventLocationId is required")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<SeatDTO> getSeatsByEventLocation(
            @QueryParam("eventLocationId") UUID eventLocationId) {
        LOG.debugf(
                "Received GET request to /api/manager/seats?eventLocationId=%s", eventLocationId);
        if (eventLocationId == null) {
            throw new ValidationException("eventLocationId query parameter is required");
        }
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        List<SeatDTO> result =
                seatService.findSeatsForManagerByLocation(eventLocationId, currentUser);
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
    public SeatDTO getManagerSeatById(@PathParam("id") UUID id) {
        LOG.debugf("Received GET request to /api/manager/seats/%s.", id);
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        SeatDTO result = seatService.findSeatByIdForManager(id, currentUser);
        if (result != null) {
            LOG.debugf("Successfully retrieved seat with ID %s.", id);
        } else {
            LOG.warnf("Seat with ID %s not found.", id);
        }
        return result;
    }
}
