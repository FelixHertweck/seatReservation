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
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.eventManagement.dto.DetailedEventResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.service.EventService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/events")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    private static final Logger LOG = Logger.getLogger(EventResource.class);

    @Inject EventService eventService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = DetailedEventResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event location not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Event with this name already exists in this event location")
    public DetailedEventResponseDTO createEvent(@Valid EventRequestDTO dto) {
        LOG.infof("Received POST request to /api/manager/events to create a new event.");
        LOG.debugf("EventRequestDTO received: %s", dto.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        DetailedEventResponseDTO result = eventService.createEvent(dto, currentUser);
        LOG.infof("Event '%s' created successfully with ID %d.", result.name(), result.id());
        return result;
    }

    @PUT
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = DetailedEventResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event or event location not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Event with this name already exists in this event location")
    public DetailedEventResponseDTO updateEvent(
            @PathParam("id") Long id, @Valid EventRequestDTO dto) {
        LOG.infof("Received PUT request to /api/manager/events/%d to update event.", id);
        LOG.debugf("EventRequestDTO received for ID %d: %s", id, dto.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        DetailedEventResponseDTO result = eventService.updateEvent(id, dto, currentUser);
        LOG.infof("Event with ID %d updated successfully.", id);
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
                                            implementation = DetailedEventResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<DetailedEventResponseDTO> getEventsByCurrentManager() {
        LOG.infof("Received GET request to /api/manager/events to get events by current manager.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<DetailedEventResponseDTO> result = eventService.getEventsByCurrentManager(currentUser);
        LOG.infof(
                "Successfully responded to GET /api/manager/events with %d events.", result.size());
        return result;
    }

    @GET
    @Path("/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = DetailedEventResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Event with specified ID not found for the current manager")
    public DetailedEventResponseDTO getEventById(@PathParam("id") Long id) {
        LOG.infof("Received GET request to /api/manager/events/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        DetailedEventResponseDTO result = eventService.getEventByIdForManager(id, currentUser);
        if (result != null) {
            LOG.infof("Successfully retrieved event with ID %d.", id);
        } else {
            LOG.warnf("Event with ID %d not found.", id);
        }
        return result;
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "Event deleted")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Event with specified ID not found for the current manager")
    public void deleteEvent(@PathParam("id") Long id) {
        LOG.infof("Received DELETE request to /api/manager/events/%d to delete event.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        eventService.deleteEvent(id, currentUser);
        LOG.infof("Event with ID %d deleted successfully.", id);
    }
}
