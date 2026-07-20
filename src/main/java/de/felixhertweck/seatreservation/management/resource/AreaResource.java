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

import de.felixhertweck.seatreservation.management.dto.AreaRequestDTO;
import de.felixhertweck.seatreservation.management.dto.AreaResponseDTO;
import de.felixhertweck.seatreservation.management.service.AreaService;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/areas")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AreaResource {

    private static final Logger LOG = Logger.getLogger(AreaResource.class);

    @Inject AreaService areaService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = AreaResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event location not found")
    public AreaResponseDTO createArea(@Valid AreaRequestDTO areaRequestDTO) {
        LOG.debugf("Received POST request to /api/manager/areas to create a new area.");
        User currentUser = userSecurityContext.getCurrentUser();
        return areaService.createArea(areaRequestDTO, currentUser);
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
                                            implementation = AreaResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Bad Request: eventLocationId is required")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<AreaResponseDTO> getAreasByEventLocation(
            @QueryParam("eventLocationId") Long eventLocationId) {
        LOG.debugf(
                "Received GET request to /api/manager/areas?eventLocationId=%d", eventLocationId);
        if (eventLocationId == null) {
            throw new IllegalArgumentException("eventLocationId query parameter is required");
        }
        User currentUser = userSecurityContext.getCurrentUser();
        return areaService.findAreasByLocation(eventLocationId, currentUser);
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = AreaResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Area with specified ID not found for the current manager")
    public AreaResponseDTO getManagerAreaById(@PathParam("id") Long id) {
        LOG.debugf("Received GET request to /api/manager/areas/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        return areaService.findAreaByIdForManager(id, currentUser);
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = AreaResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Area with specified ID not found for the current manager")
    public AreaResponseDTO updateManagerArea(
            @PathParam("id") Long id, @Valid AreaRequestDTO areaRequestDTO) {
        LOG.debugf("Received PUT request to /api/manager/areas/%d to update area.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        return areaService.updateArea(id, areaRequestDTO, currentUser);
    }

    @DELETE
    @APIResponse(responseCode = "204", description = "Area(s) deleted successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid input")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Area with specified ID not found for the current manager")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Area is still referenced by at least one seat")
    public void deleteManagerArea(@QueryParam("ids") List<Long> ids) {
        LOG.debugf(
                "Received DELETE request to /api/manager/areas with IDs: %s",
                ids != null ? ids : Collections.emptyList());
        User currentUser = userSecurityContext.getCurrentUser();
        areaService.deleteAreas(ids, currentUser);
    }
}
