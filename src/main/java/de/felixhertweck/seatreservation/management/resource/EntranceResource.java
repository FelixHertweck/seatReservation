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
import java.util.UUID;
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

import de.felixhertweck.seatreservation.management.dto.EntranceRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EntranceResponseDTO;
import de.felixhertweck.seatreservation.management.service.EntranceService;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/entrances")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EntranceResource {

    private static final Logger LOG = Logger.getLogger(EntranceResource.class);

    @Inject EntranceService entranceService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EntranceResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event location not found")
    public EntranceResponseDTO createEntrance(@Valid EntranceRequestDTO entranceRequestDTO) {
        LOG.debugf("Received POST request to /api/manager/entrances to create a new entrance.");
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        return entranceService.createEntrance(entranceRequestDTO, currentUser);
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
                                            implementation = EntranceResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Bad Request: eventLocationId is required")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<EntranceResponseDTO> getEntrancesByEventLocation(
            @QueryParam("eventLocationId") UUID eventLocationId) {
        LOG.debugf(
                "Received GET request to /api/manager/entrances?eventLocationId=%s",
                eventLocationId);
        if (eventLocationId == null) {
            throw new IllegalArgumentException("eventLocationId query parameter is required");
        }
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        return entranceService.findEntrancesByLocation(eventLocationId, currentUser);
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EntranceResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Entrance with specified ID not found for the current manager")
    public EntranceResponseDTO getManagerEntranceById(@PathParam("id") UUID id) {
        LOG.debugf("Received GET request to /api/manager/entrances/%s.", id);
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        return entranceService.findEntranceByIdForManager(id, currentUser);
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EntranceResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Entrance with specified ID not found for the current manager")
    public EntranceResponseDTO updateManagerEntrance(
            @PathParam("id") UUID id, @Valid EntranceRequestDTO entranceRequestDTO) {
        LOG.debugf("Received PUT request to /api/manager/entrances/%s to update entrance.", id);
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        return entranceService.updateEntrance(id, entranceRequestDTO, currentUser);
    }

    @DELETE
    @APIResponse(responseCode = "204", description = "Entrance(s) deleted successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid input")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Entrance with specified ID not found for the current manager")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Entrance is still referenced by at least one seat")
    public void deleteManagerEntrance(@QueryParam("ids") List<UUID> ids) {
        LOG.debugf(
                "Received DELETE request to /api/manager/entrances with IDs: %s",
                ids != null ? ids : Collections.emptyList());
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        entranceService.deleteEntrances(ids, currentUser);
    }
}
