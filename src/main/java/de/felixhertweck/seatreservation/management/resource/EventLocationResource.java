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

import de.felixhertweck.seatreservation.management.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.management.dto.EventLocationUpdateDTO;
import de.felixhertweck.seatreservation.management.service.EventLocationService;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/eventlocations")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventLocationResource {

    private static final Logger LOG = Logger.getLogger(EventLocationResource.class);

    @Inject EventLocationService eventLocationService;

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
                                            implementation = EventLocationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<EventLocationResponseDTO> getEventLocationsByCurrentManager() {
        LOG.debugf("Received GET request to /api/manager/eventlocations");
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        List<EventLocationResponseDTO> result =
                eventLocationService.getEventLocationsByCurrentManager(currentUser);
        LOG.debugf(
                "Successfully responded to GET /api/manager/eventlocations with %d event"
                        + " locations.",
                result.size());
        return result;
    }

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Event location with this name already exists")
    public EventLocationResponseDTO createEventLocation(@Valid EventLocationRequestDTO dto) {
        LOG.debugf("Received POST request to /api/manager/eventlocations for new event location.");
        LOG.debugf("EventLocationRequestDTO received: %s", dto.toString());
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        EventLocationResponseDTO result =
                eventLocationService.createEventLocation(dto, currentUser);
        LOG.infof("Event location '%s' created successfully.", result.name());
        return result;
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Event location with specified ID not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Event location with this name already exists")
    public EventLocationResponseDTO updateEventLocation(
            @PathParam("id") UUID id, @Valid EventLocationUpdateDTO dto) {
        LOG.debugf(
                "Received PUT request to /api/manager/eventlocations/%s to update event location.",
                id);
        LOG.debugf("EventLocationRequestDTO received for ID %s: %s", id, dto.toString());
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        EventLocationResponseDTO result =
                eventLocationService.updateEventLocation(id, dto, currentUser);
        LOG.infof("Event location with ID %s updated successfully.", id);
        return result;
    }

    @DELETE
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "204", description = "Event location deleted successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid input")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Event location with specified ID not found")
    public void deleteEventLocation(@QueryParam("ids") List<UUID> ids) {
        LOG.debugf(
                "Received DELETE request to /api/manager/eventlocations with IDs: %s",
                ids != null ? ids : Collections.emptyList());
        AuthenticatedUser currentUser = userSecurityContext.getAuthenticatedUser();
        eventLocationService.deleteEventLocation(ids, currentUser);
        LOG.debugf(
                "Event location with IDs %s deleted successfully.",
                ids != null ? ids : Collections.emptyList());
    }
}
