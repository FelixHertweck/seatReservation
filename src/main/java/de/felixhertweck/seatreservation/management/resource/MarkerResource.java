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

import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.management.dto.MakerRequestDTO;
import de.felixhertweck.seatreservation.management.service.MarkerService;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/markers")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MarkerResource {

    private static final Logger LOG = Logger.getLogger(MarkerResource.class);

    @Inject MarkerService markerService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationMakerDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event location not found")
    public EventLocationMakerDTO createMarker(@Valid MakerRequestDTO markerRequestDTO) {
        LOG.debugf("Received POST request to /api/manager/markers to create a new marker.");
        User currentUser = userSecurityContext.getCurrentUser();
        return markerService.createMarker(markerRequestDTO, currentUser);
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
                                            implementation = EventLocationMakerDTO.class)))
    @APIResponse(responseCode = "400", description = "Bad Request: eventLocationId is required")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<EventLocationMakerDTO> getMarkersByEventLocation(
            @QueryParam("eventLocationId") Long eventLocationId) {
        LOG.debugf(
                "Received GET request to /api/manager/markers?eventLocationId=%d", eventLocationId);
        if (eventLocationId == null) {
            throw new IllegalArgumentException("eventLocationId query parameter is required");
        }
        User currentUser = userSecurityContext.getCurrentUser();
        return markerService.findMarkersByLocation(eventLocationId, currentUser);
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationMakerDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Marker with specified ID not found for the current manager")
    public EventLocationMakerDTO getManagerMarkerById(@PathParam("id") Long id) {
        LOG.debugf("Received GET request to /api/manager/markers/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        return markerService.findMarkerByIdForManager(id, currentUser);
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationMakerDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Marker with specified ID not found for the current manager")
    public EventLocationMakerDTO updateManagerMarker(
            @PathParam("id") Long id, @Valid MakerRequestDTO markerRequestDTO) {
        LOG.debugf("Received PUT request to /api/manager/markers/%d to update marker.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        return markerService.updateMarker(id, markerRequestDTO, currentUser);
    }

    @DELETE
    @APIResponse(responseCode = "204", description = "Marker(s) deleted successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid input")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Marker with specified ID not found for the current manager")
    public void deleteManagerMarker(@QueryParam("ids") List<Long> ids) {
        LOG.debugf(
                "Received DELETE request to /api/manager/markers with IDs: %s",
                ids != null ? ids : Collections.emptyList());
        User currentUser = userSecurityContext.getCurrentUser();
        markerService.deleteMarkers(ids, currentUser);
    }
}
