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

import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowanceUpdateDto;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesCreateDto;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesResponseDto;
import de.felixhertweck.seatreservation.eventManagement.service.EventReservationAllowanceService;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/reservationAllowance")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventReservationAllowanceResource {

    private static final Logger LOG = Logger.getLogger(EventReservationAllowanceResource.class);

    @Inject EventReservationAllowanceService eventReservationAllowanceService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = EventUserAllowancesResponseDto.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event or user not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Allowance already exists for this user and event")
    public List<EventUserAllowancesResponseDto> setReservationsAllowedForUser(
            @Valid EventUserAllowancesCreateDto dto) {
        LOG.debugf(
                "Received POST request to /api/manager/reservationAllowance to set reservation"
                        + " allowance.");
        User currentUser = userSecurityContext.getCurrentUser();
        Set<EventUserAllowance> result =
                eventReservationAllowanceService.setReservationsAllowedForUser(
                        dto.getEventId(),
                        dto.getUserIds(),
                        dto.getReservationsAllowedCount(),
                        currentUser);
        LOG.debugf(
                "Reservation allowance set successfully for user IDs %s and event ID %d.",
                dto.getUserIds(), dto.getEventId());
        return result.stream().map(EventUserAllowancesResponseDto::toDTO).toList();
    }

    @PUT
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(implementation = EventUserAllowancesResponseDto.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Allowance with specified ID not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Allowance already exists for this user and event")
    public EventUserAllowancesResponseDto updateReservationAllowance(
            @Valid EventUserAllowanceUpdateDto dto) {
        LOG.debugf(
                "Received PUT request to /api/manager/reservationAllowance to update reservation"
                        + " allowance with ID %d.",
                dto.id());
        User currentUser = userSecurityContext.getCurrentUser();
        EventUserAllowance result =
                eventReservationAllowanceService.updateReservationAllowance(
                        dto.id(),
                        dto.eventId(),
                        dto.userId(),
                        dto.reservationsAllowedCount(),
                        currentUser);
        LOG.debugf("Reservation allowance with ID %d updated successfully.", dto.id());
        return EventUserAllowancesResponseDto.toDTO(result);
    }

    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "Reservation allowance retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Allowance with specified ID not found")
    public EventUserAllowancesResponseDto getReservationAllowanceById(@PathParam("id") Long id) {
        LOG.debugf("Received GET request to /api/manager/reservationAllowance/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        EventUserAllowance result =
                eventReservationAllowanceService.getReservationAllowanceById(id, currentUser);

        LOG.debugf("Successfully retrieved reservation allowance with ID %d.", id);
        return EventUserAllowancesResponseDto.toDTO(result);
    }

    @GET
    @APIResponse(
            responseCode = "200",
            description = "List of reservation allowances retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<EventUserAllowancesResponseDto> getReservationAllowances() {
        LOG.debugf(
                "Received GET request to /api/manager/reservationAllowance to get all allowances.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<EventUserAllowance> result =
                eventReservationAllowanceService.getReservationAllowances(currentUser);
        LOG.debugf("Successfully retrieved %d reservation allowances.", result.size());
        return result.stream().map(EventUserAllowancesResponseDto::toDTO).toList();
    }

    @GET
    @Path("/event/{eventId}")
    @APIResponse(
            responseCode = "200",
            description =
                    "List of reservation allowances for a specific event retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event with specified ID not found")
    public List<EventUserAllowancesResponseDto> getReservationAllowancesByEventId(
            @PathParam("eventId") Long eventId) {
        LOG.debugf("Received GET request to /api/manager/reservationAllowance/event/%d.", eventId);
        User currentUser = userSecurityContext.getCurrentUser();
        List<EventUserAllowance> result =
                eventReservationAllowanceService.getReservationAllowancesByEventId(
                        eventId, currentUser);
        LOG.debugf(
                "Successfully retrieved %d reservation allowances for event ID %d.",
                result.size(), eventId);
        return result.stream().map(EventUserAllowancesResponseDto::toDTO).toList();
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "No Content")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Allowance with specified ID not found")
    public void deleteReservationAllowance(@PathParam("id") Long id) {
        LOG.debugf(
                "Received DELETE request to /api/manager/reservationAllowance/%d to delete"
                        + " allowance.",
                id);
        User currentUser = userSecurityContext.getCurrentUser();
        eventReservationAllowanceService.deleteReservationAllowance(id, currentUser);
        LOG.debugf("Reservation allowance with ID %d deleted successfully.", id);
    }
}
