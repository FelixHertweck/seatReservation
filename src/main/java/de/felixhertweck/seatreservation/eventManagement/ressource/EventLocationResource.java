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
import java.util.Set;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.ImportEventLocationDto;
import de.felixhertweck.seatreservation.eventManagement.dto.ImportSeatDto;
import de.felixhertweck.seatreservation.eventManagement.service.EventLocationService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
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
    public List<EventLocationResponseDTO> getEventLocationsByCurrentManager() {
        LOG.infof("Received GET request to /api/manager/eventlocations");
        User currentUser = userSecurityContext.getCurrentUser();
        List<EventLocationResponseDTO> result =
                eventLocationService.getEventLocationsByCurrentManager(currentUser);
        LOG.infof(
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
    public EventLocationResponseDTO createEventLocation(@Valid EventLocationRequestDTO dto) {
        LOG.infof("Received POST request to /api/manager/eventlocations for new event location.");
        LOG.debugf("EventLocationRequestDTO received: %s", dto.toString());
        User currentUser = userSecurityContext.getCurrentUser();
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
    public EventLocationResponseDTO updateEventLocation(
            @PathParam("id") Long id, @Valid EventLocationRequestDTO dto) {
        LOG.infof(
                "Received PUT request to /api/manager/eventlocations/%d to update event location.",
                id);
        LOG.debugf("EventLocationRequestDTO received for ID %d: %s", id, dto.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        EventLocationResponseDTO result =
                eventLocationService.updateEventLocation(id, dto, currentUser);
        LOG.infof("Event location with ID %d updated successfully.", id);
        return result;
    }

    @DELETE
    @APIResponse(responseCode = "200", description = "OK")
    @Path("/{id}")
    public void deleteEventLocation(@PathParam("id") Long id) {
        LOG.infof(
                "Received DELETE request to /api/manager/eventlocations/%d to delete event"
                        + " location.",
                id);
        User currentUser = userSecurityContext.getCurrentUser();
        eventLocationService.deleteEventLocation(id, currentUser);
        LOG.infof("Event location with ID %d deleted successfully.", id);
    }

    @POST
    @Path("/import")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationResponseDTO.class)))
    public EventLocationResponseDTO createEventLocationWithSeats(
            @Valid ImportEventLocationDto dto) {
        LOG.infof(
                "Received POST request to /api/manager/eventlocations/register for new event"
                        + " location with seats.");
        LOG.debugf("EventLocationRegistrationDTO received: %s", dto.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        EventLocationResponseDTO result =
                eventLocationService.importEventLocation(dto, currentUser);
        LOG.infof("Event location '%s' with seats created successfully.", result.name());
        return result;
    }

    @POST
    @Path("/import/{id}")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventLocationResponseDTO.class)))
    public EventLocationResponseDTO importSeatsToEventLocation(
            @PathParam("id") Long id, @Valid Set<ImportSeatDto> seats) {
        LOG.infof(
                "Received POST request to /api/manager/eventlocations/import for new event"
                        + " location with seats.");
        LOG.debugf("SeatRequestDTO received: %s", seats.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        EventLocationResponseDTO result =
                eventLocationService.importSeatsToEventLocation(id, seats, currentUser);
        LOG.infof("Event location '%s' with seats created successfully.", result.name());
        return result;
    }
}
